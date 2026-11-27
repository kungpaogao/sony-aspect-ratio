package org.andrewgao.sony.aspectratio.camera

import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.CameraKeys
import com.github.ma1co.openmemories.framework.CameraParameters
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.andrewgao.sony.aspectratio.shooting.DialAction
import org.andrewgao.sony.aspectratio.shooting.DialReconciler
import org.andrewgao.sony.aspectratio.shooting.ExposureSteps
import org.andrewgao.sony.aspectratio.shooting.Focus
import org.andrewgao.sony.aspectratio.shooting.FocusMode
import org.andrewgao.sony.aspectratio.shooting.IsoSteps
import org.andrewgao.sony.aspectratio.shooting.ShutterSpeed

data class Exposure(
    val sceneMode: String? = null,
    val shutterNumerator: Int = 0,
    val shutterDenominator: Int = 0,
    val apertureX100: Int = 0,
    val evIndex: Int = 0,
    val evStep: Float = 0f,
    val iso: Int = 0,
) {
    val isBulb: Boolean
        get() = shutterNumerator == BULB_NUMERATOR && shutterDenominator == 1

    companion object {
        /** Sony reports BULB as the shutter speed 65535/1. */
        const val BULB_NUMERATOR = 65535
    }
}

/** Live exposure read from the open camera's parameters and shutter/aperture listeners. */
class ShootingControls {
    private val mutableExposure = MutableStateFlow(Exposure())
    val exposure: StateFlow<Exposure> = mutableExposure

    private val mutableFocus = MutableStateFlow(Focus.IDLE)
    val focus: StateFlow<Focus> = mutableFocus

    private val mutableFocusMode = MutableStateFlow(FocusMode.UNKNOWN)
    val focusMode: StateFlow<FocusMode> = mutableFocusMode

    val magnifier = Magnifier { mutableFocusMode.value }

    private var camera: Camera? = null
    private val loggedFailures = mutableSetOf<String>()
    private val main = Handler(Looper.getMainLooper())

    // The listeners may fire on another thread, so everything below the lock is shared.
    private val lock = Any()
    private val apertureDial = DialReconciler<Int>(same = ExposureSteps::sameAperture)
    private val shutterDial = DialReconciler<ShutterSpeed>(same = ExposureSteps::sameShutter)
    private val settleApertureTask = Runnable { settle(aperture = true) }
    private val settleShutterTask = Runnable { settle(aperture = false) }

    private var supportedIso = emptyList<Int>()
    private var isoTarget: Int? = null
    private val applyIsoTask = Runnable { applyIso() }

    private var evRange: IntRange? = null

    fun attach(active: Camera) {
        camera = active
        loggedFailures.clear()
        synchronized(lock) {
            apertureDial.reset()
            shutterDial.reset()
            isoTarget = null
        }
        evRange = null
        refresh()
        readFocusMode(active)
        readIsoList(active)
        active.setShutterSpeedListener { n, d -> onShutterReport(n, d) }
        active.setApertureListener { ap -> onApertureReport(ap) }
        active.setAutoFocusListener { success ->
            Trace.line("focus result success=$success")
            // A result that arrives after AEL was released must not bring the indicator back.
            if (mutableFocus.value == Focus.FOCUSING) {
                mutableFocus.value = if (success) Focus.LOCKED else Focus.FAILED
            }
        }
        applyEvDial()
        val e = mutableExposure.value
        Trace.line(
            "exposure mode=${e.sceneMode} ${e.shutterNumerator}/${e.shutterDenominator} " +
                "f/${e.apertureX100} ev=${e.evIndex}x${e.evStep} iso=${e.iso}",
        )
        magnifier.attach(active)
    }

    fun detach() {
        flushIso()
        magnifier.detach()
        cancelFocus()
        camera?.setShutterSpeedListener(null)
        camera?.setApertureListener(null)
        camera?.setAutoFocusListener(null)
        main.removeCallbacks(settleApertureTask)
        main.removeCallbacks(settleShutterTask)
        main.removeCallbacks(applyIsoTask)
        camera = null
        mutableFocusMode.value = FocusMode.UNKNOWN
        loggedFailures.clear()
    }

    fun focus() {
        val active = camera ?: return
        try {
            Trace.line("focus press")
            mutableFocus.value = Focus.FOCUSING
            active.autoFocus()
        } catch (error: Exception) {
            Trace.line("focus " + error.message)
            mutableFocus.value = Focus.FAILED
        }
    }

    fun cancelFocus() {
        if (mutableFocus.value == Focus.IDLE) return
        val active = camera
        try {
            Trace.line("focus release")
            active?.cancelAutoFocus()
        } catch (error: Exception) {
            Trace.line("focus cancel " + error.message)
        }
        mutableFocus.value = Focus.IDLE
    }

    fun apply(action: DialAction) {
        Trace.line("dial $action")
        val active = camera ?: return
        val started = SystemClock.uptimeMillis()
        try {
            when (action) {
                is DialAction.ProgramShift -> {
                    active.adjustProgramShift(action.steps)
                    refresh()
                }
                is DialAction.Aperture -> stepAperture(active, action.steps)
                is DialAction.Shutter -> stepShutter(active, action.steps)
                is DialAction.Iso -> stepIso(action.steps)
            }
        } catch (error: Exception) {
            Trace.line("dial " + error.message)
        }
        Log.i(TAG, "dial $action ${SystemClock.uptimeMillis() - started}ms")
    }

    fun applyEvDial() {
        val active = camera ?: return
        try {
            val position = CameraKeys.getEvDialPosition()
            val known = evRange
            if (known != null) publishEv(position.coerceIn(known.first, known.last))
            val params = read("parameters", null) { active.parameters } ?: return
            val min = read("minEv", 0) { params.minExposureCompensation }
            val max = read("maxEv", 0) { params.maxExposureCompensation }
            evRange = min..max
            val index = position.coerceIn(min, max)
            publishEv(index)
            params.exposureCompensation = index
            active.setParameters(params)
        } catch (error: Exception) {
            Trace.line("ev dial " + error.message)
            refresh()
        }
    }

    private fun publishEv(index: Int) {
        mutableExposure.value = mutableExposure.value.copy(evIndex = index)
    }

    private fun stepAperture(active: Camera, steps: Int) {
        val shown = mutableExposure.value
        val next =
            ExposureSteps.nextAperture(
                shown.apertureX100,
                steps,
                ExposureSteps.isHalfStop(shown.evStep),
                active.apertureRange,
            )
        if (next != null) {
            synchronized(lock) {
                apertureDial.step(next, SystemClock.uptimeMillis())
                mutableExposure.value = mutableExposure.value.copy(apertureX100 = next)
            }
            main.removeCallbacks(settleApertureTask)
            main.postDelayed(settleApertureTask, DialReconciler.SETTLE_MS)
        }
        repeat(abs(steps)) {
            if (steps > 0) active.incrementAperture() else active.decrementAperture()
        }
    }

    /** Sony skips shutter steps in BULB. */
    private fun stepShutter(active: Camera, steps: Int) {
        val shown = mutableExposure.value
        if (shown.isBulb) return
        val next =
            ExposureSteps.nextShutter(
                shown.shutterNumerator,
                shown.shutterDenominator,
                steps,
                ExposureSteps.isHalfStop(shown.evStep),
                active.shutterSpeedRange,
            )
        if (next != null) {
            synchronized(lock) {
                shutterDial.step(next, SystemClock.uptimeMillis())
                mutableExposure.value =
                    mutableExposure.value.copy(
                        shutterNumerator = next.numerator,
                        shutterDenominator = next.denominator,
                    )
            }
            main.removeCallbacks(settleShutterTask)
            main.postDelayed(settleShutterTask, DialReconciler.SETTLE_MS)
        }
        repeat(abs(steps)) {
            if (steps > 0) active.incrementShutterSpeed() else active.decrementShutterSpeed()
        }
    }

    private fun onApertureReport(ap: Int) {
        synchronized(lock) {
            val value = apertureDial.report(ap, SystemClock.uptimeMillis())
            mutableExposure.value = mutableExposure.value.copy(apertureX100 = value)
        }
    }

    private fun onShutterReport(n: Int, d: Int) {
        synchronized(lock) {
            val value = shutterDial.report(ShutterSpeed(n, d), SystemClock.uptimeMillis())
            mutableExposure.value =
                mutableExposure.value.copy(
                    shutterNumerator = value.numerator,
                    shutterDenominator = value.denominator,
                )
        }
    }

    /** Runs a while after a step; lets the camera's own value replace the stepped one. */
    private fun settle(aperture: Boolean) {
        val done =
            synchronized(lock) {
                val now = SystemClock.uptimeMillis()
                if (aperture) {
                    val done = apertureDial.settle(now)
                    apertureDial.shown?.let {
                        mutableExposure.value = mutableExposure.value.copy(apertureX100 = it)
                    }
                    done
                } else {
                    val done = shutterDial.settle(now)
                    shutterDial.shown?.let {
                        mutableExposure.value =
                            mutableExposure.value.copy(
                                shutterNumerator = it.numerator,
                                shutterDenominator = it.denominator,
                            )
                    }
                    done
                }
            }
        if (!done && camera != null) {
            val task = if (aperture) settleApertureTask else settleShutterTask
            main.removeCallbacks(task)
            main.postDelayed(task, DialReconciler.SETTLE_MS)
        }
    }

    /**
     * Shows the new ISO at once; the camera write waits until the wheel rests. The write blocks the
     * main thread for about 110 ms, and the camera holds back wheel clicks meanwhile, so writing
     * during a spin would stretch the readout out for seconds.
     */
    private fun stepIso(steps: Int) {
        val next = IsoSteps.next(supportedIso, mutableExposure.value.iso, steps) ?: return
        mutableExposure.value = mutableExposure.value.copy(iso = next)
        isoTarget = next
        main.removeCallbacks(applyIsoTask)
        main.postDelayed(applyIsoTask, ISO_WRITE_DELAY_MS)
    }

    /** Writes a pending ISO now, before anything that depends on the real value. */
    fun flushIso() {
        if (isoTarget == null) return
        main.removeCallbacks(applyIsoTask)
        applyIso()
    }

    private fun applyIso() {
        val active = camera ?: return
        val target = isoTarget ?: return
        isoTarget = null
        val started = SystemClock.uptimeMillis()
        try {
            val params = read("parameters", null) { active.parameters } ?: return
            params.iso = target
            active.setParameters(params)
        } catch (error: Exception) {
            Trace.line("iso " + error.message)
            refresh()
        }
        Log.i(TAG, "iso $target ${SystemClock.uptimeMillis() - started}ms")
    }

    private fun readIsoList(active: Camera) {
        val params = read("parameters", null) { active.parameters } ?: return
        supportedIso = read("supportedIso", emptyList<Int>()) { params.supportedIso }
    }

    /** Re-reads every value from the camera parameters. */
    fun refresh() {
        val active = camera ?: return
        val params = read("parameters", null) { active.parameters } ?: return
        val fresh =
            Exposure(
                sceneMode = read("sceneMode", null) { params.sceneMode },
                shutterNumerator = read("shutterNumerator", 0) { params.shutterNumerator },
                shutterDenominator = read("shutterDenominator", 0) { params.shutterDenominator },
                apertureX100 = read("aperture", 0) { params.aperture },
                evIndex = read("evIndex", 0) { params.exposureCompensation },
                evStep = read("evStep", 0f) { params.exposureCompensationStep },
                iso = read("iso", 0) { params.iso },
            )
        synchronized(lock) {
            // The parameters lag the dials, so they go through the same reconciling as the
            // listeners; an ISO write still waiting to run keeps the shown value.
            val now = SystemClock.uptimeMillis()
            val aperture =
                if (fresh.apertureX100 > 0) apertureDial.report(fresh.apertureX100, now)
                else fresh.apertureX100
            val shutter =
                if (fresh.shutterDenominator > 0) {
                    shutterDial.report(
                        ShutterSpeed(fresh.shutterNumerator, fresh.shutterDenominator),
                        now,
                    )
                } else {
                    ShutterSpeed(fresh.shutterNumerator, fresh.shutterDenominator)
                }
            mutableExposure.value =
                fresh.copy(
                    apertureX100 = aperture,
                    shutterNumerator = shutter.numerator,
                    shutterDenominator = shutter.denominator,
                    iso = if (isoTarget != null) mutableExposure.value.iso else fresh.iso,
                )
        }
    }

    /**
     * Read once per attach, not on every [refresh]: nothing in the app changes the focus mode, and
     * an earlier camera hang was suspected (never confirmed) to come from Sony's getAutoFocusMode.
     */
    private fun readFocusMode(active: Camera) {
        val params = read("parameters", null) { active.parameters } ?: return
        val raw = read("focusMode", null) { params.focusMode }
        // Like Sony's apps, read the AF mode only when the focus mode is auto.
        val rawAuto =
            if (raw == CameraParameters.FOCUS_AUTO) {
                read("autoFocusMode", null) { params.autoFocusMode }
            } else {
                null
            }
        val mode = FocusMode.from(raw, rawAuto)
        if (mode != mutableFocusMode.value) {
            Trace.line("focus mode $raw/$rawAuto -> $mode")
            mutableFocusMode.value = mode
        }
    }

    /**
     * Reads one value; a getter the emulator doesn't support falls back instead of failing the
     * rest.
     */
    private inline fun <T> read(name: String, fallback: T, getter: () -> T): T =
        try {
            getter()
        } catch (error: Exception) {
            logFailureOnce(name, error)
            fallback
        }

    private fun logFailureOnce(name: String, error: Exception) {
        if (loggedFailures.add(name)) {
            Trace.line("exposure $name " + error.message)
        }
    }

    private companion object {
        const val TAG = "AspectRatioTrace"
        const val ISO_WRITE_DELAY_MS = 250L
    }
}
