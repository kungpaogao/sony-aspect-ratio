package org.andrewgao.sony.aspectratio.camera

import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.CameraKeys
import kotlin.math.abs
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.andrewgao.sony.aspectratio.shooting.DialAction
import org.andrewgao.sony.aspectratio.shooting.Focus

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

    private var camera: Camera? = null
    private val loggedFailures = mutableSetOf<String>()

    fun attach(active: Camera) {
        camera = active
        loggedFailures.clear()
        refresh()
        active.setShutterSpeedListener { n, d ->
            mutableExposure.value =
                mutableExposure.value.copy(shutterNumerator = n, shutterDenominator = d)
        }
        active.setApertureListener { ap ->
            mutableExposure.value = mutableExposure.value.copy(apertureX100 = ap)
        }
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
    }

    fun detach() {
        cancelFocus()
        camera?.setShutterSpeedListener(null)
        camera?.setApertureListener(null)
        camera?.setAutoFocusListener(null)
        camera = null
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
        try {
            when (action) {
                is DialAction.ProgramShift -> active.adjustProgramShift(action.steps)
                is DialAction.Aperture -> stepAperture(active, action.steps)
                is DialAction.Shutter -> stepShutter(active, action.steps)
                is DialAction.Iso -> stepIso(active, action.steps)
            }
        } catch (error: Exception) {
            Trace.line("dial " + error.message)
        }
        refresh()
    }

    fun applyEvDial() {
        val active = camera ?: return
        val params = read("parameters", null) { active.parameters } ?: return
        try {
            val position = CameraKeys.getEvDialPosition()
            val min = read("minEv", 0) { params.minExposureCompensation }
            val max = read("maxEv", 0) { params.maxExposureCompensation }
            params.exposureCompensation = position.coerceIn(min, max)
            active.setParameters(params)
        } catch (error: Exception) {
            Trace.line("ev dial " + error.message)
        }
        refresh()
    }

    private fun stepAperture(active: Camera, steps: Int) {
        repeat(abs(steps)) {
            if (steps > 0) active.incrementAperture() else active.decrementAperture()
        }
    }

    /** Sony skips shutter steps in BULB. */
    private fun stepShutter(active: Camera, steps: Int) {
        if (mutableExposure.value.isBulb) return
        repeat(abs(steps)) {
            if (steps > 0) active.incrementShutterSpeed() else active.decrementShutterSpeed()
        }
    }

    private fun stepIso(active: Camera, steps: Int) {
        val params = read("parameters", null) { active.parameters } ?: return
        val supported = read("supportedIso", emptyList<Int>()) { params.supportedIso }
        if (supported.isEmpty()) return
        val current = read("iso", supported[0]) { params.iso }
        var index = supported.indexOf(current)
        if (index < 0) {
            index = supported.indices.minByOrNull { abs(supported[it] - current) } ?: 0
        }
        val newIndex = (index + steps).coerceIn(0, supported.lastIndex)
        if (newIndex == index) return
        params.iso = supported[newIndex]
        active.setParameters(params)
    }

    /** Re-reads every value from the camera parameters. */
    fun refresh() {
        val active = camera ?: return
        val params = read("parameters", null) { active.parameters } ?: return
        mutableExposure.value =
            Exposure(
                sceneMode = read("sceneMode", null) { params.sceneMode },
                shutterNumerator = read("shutterNumerator", 0) { params.shutterNumerator },
                shutterDenominator = read("shutterDenominator", 0) { params.shutterDenominator },
                apertureX100 = read("aperture", 0) { params.aperture },
                evIndex = read("evIndex", 0) { params.exposureCompensation },
                evStep = read("evStep", 0f) { params.exposureCompensationStep },
                iso = read("iso", 0) { params.iso },
            )
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
}
