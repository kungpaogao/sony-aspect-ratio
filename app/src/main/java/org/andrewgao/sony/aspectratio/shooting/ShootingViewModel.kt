package org.andrewgao.sony.aspectratio.shooting

import android.content.Context
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.CameraKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.camera.CropSession
import org.andrewgao.sony.aspectratio.camera.DeviceStatus
import org.andrewgao.sony.aspectratio.camera.LiveViewArea
import org.andrewgao.sony.aspectratio.camera.Trace
import org.andrewgao.sony.aspectratio.shared.ViewModel

class ShootingViewModel(
    private val context: Context,
    recreateSurface: () -> Unit,
) : ViewModel<ShootingState>(ShootingState()) {
    private val session =
        CropSession.create(recreateSurface = recreateSurface, onStatus = Trace::line)
    private val liveViewArea = LiveViewArea()
    private val deviceStatus = DeviceStatus()
    private var shot: Job? = null
    private var holder: SurfaceHolder? = null
    private var started = false

    /**
     * What the AF/MF/AEL button's current press does, chosen on key down. Back-button focus: AEL
     * holds focus and the shutter shoots with it; half-press does nothing.
     */
    private var pressedButton: FocusButton? = null

    init {
        scope.launch {
            liveViewArea.area.collect { area -> setState { copy(liveView = area) } }
        }
        scope.launch {
            session.controls.exposure.collect { exp ->
                val mode = ExposureMode.fromSceneMode(exp.sceneMode)
                setState {
                    copy(
                        mode = mode.label,
                        exposureMode = mode,
                        shutter =
                            ExposureFormat.shutter(exp.shutterNumerator, exp.shutterDenominator),
                        aperture = ExposureFormat.aperture(exp.apertureX100),
                        exposure = ExposureFormat.exposure(exp.evIndex, exp.evStep),
                        iso = ExposureFormat.iso(exp.iso),
                    )
                }
            }
        }
        scope.launch {
            deviceStatus.battery.collect { percent ->
                setState {
                    copy(
                        battery = ExposureFormat.battery(percent),
                        batteryLevel = ExposureFormat.batteryLevel(percent),
                    )
                }
            }
        }
        scope.launch {
            deviceStatus.shots.collect { count ->
                setState { copy(shots = ExposureFormat.shots(count)) }
            }
        }
        scope.launch {
            session.controls.focus.collect { focus -> setState { copy(focus = focus) } }
        }
        scope.launch {
            session.controls.focusMode.collect { mode -> setState { copy(focusMode = mode) } }
        }
        scope.launch {
            session.controls.magnifier.state.collect { magnification ->
                setState { copy(magnification = magnification) }
            }
        }
    }

    fun onSurface(surfaceHolder: SurfaceHolder?) {
        holder = surfaceHolder
        openPreviewIfReady()
    }

    fun clearSurface(surfaceHolder: SurfaceHolder) {
        if (holder === surfaceHolder) {
            holder = null
        }
    }

    override fun onStart() {
        started = true
        liveViewArea.start(context)
        deviceStatus.start(context)
        openPreviewIfReady()
    }

    override fun onStop() {
        started = false
        liveViewArea.stop()
        deviceStatus.stop()
        shot?.cancel()
        shot = null
        // release() detaches the controls, which cancels any focus in progress.
        session.release()
    }

    fun onKeyDown(key: Int): Boolean {
        if (key == CameraKeys.SLIDE_AEL_FOCUS) {
            Trace.line("lever " + CameraKeys.getStatus(CameraKeys.SLIDE_AEL_FOCUS))
            return true
        }
        // Only the a7R's shared button (638) depends on the lever.
        val lever =
            if (key == CameraKeys.AF_MF_AEL) CameraKeys.getStatus(CameraKeys.SLIDE_AEL_FOCUS) else 0
        val button = focusButton(key, lever)
        if (button != null) {
            Trace.line("focus button $key lever=$lever -> $button")
            pressedButton = button
            when (button) {
                FocusButton.AUTOFOCUS -> session.controls.focus()
                FocusButton.MAGNIFY -> session.controls.magnifier.cycle()
            }
            return true
        }
        if (state.value.magnification.on) {
            val magnifier = session.controls.magnifier
            when (key) {
                CameraKeys.UP -> magnifier.move(0, -1)
                CameraKeys.DOWN -> magnifier.move(0, 1)
                CameraKeys.LEFT -> magnifier.move(-1, 0)
                CameraKeys.RIGHT -> magnifier.move(1, 0)
                CameraKeys.ENTER -> magnifier.recenter()
                else -> return onKeyDownUnmagnified(key)
            }
            return true
        }
        return onKeyDownUnmagnified(key)
    }

    private fun onKeyDownUnmagnified(key: Int): Boolean {
        if (key == CameraKeys.ENTER) {
            setState { copy(choosingRatio = !choosingRatio) }
            return true
        }
        if (state.value.choosingRatio) {
            val dial = dialStep(key)
            if (dial != 0) {
                setState {
                    copy(aspect = AspectRatio.step(aspect, dial))
                }
                return true
            }
            return false
        }
        if (key == CameraKeys.EV_DIAL) {
            session.controls.applyEvDial()
            return true
        }
        if (key == CameraKeys.MODE_DIAL) {
            session.controls.refresh()
            return true
        }
        val action = dialAction(state.value.exposureMode, key)
        if (action != null) {
            session.controls.apply(action)
            return true
        }
        return false
    }

    fun onKeyUp(key: Int): Boolean {
        if (key == CameraKeys.SLIDE_AEL_FOCUS) return true
        if (focusButton(key, 0) == null) return false
        // Use what the press chose, so up matches down even if the lever moved while it was held.
        val button = pressedButton
        pressedButton = null
        if (button == FocusButton.AUTOFOCUS) {
            session.controls.cancelFocus()
        }
        return true
    }

    fun shoot() {
        if (shot?.isActive == true) return
        val aspect = state.value.aspect
        Trace.line("shutter " + aspect.label)
        // Sony leaves magnification before shooting.
        if (state.value.magnification.on) session.controls.magnifier.stop()
        session.controls.flushIso()
        shot = scope.launch {
            setState { copy(shooting = true) }
            try {
                Trace.line(session.takePicture(context, aspect))
            } finally {
                shot = null
                setState { copy(shooting = false) }
            }
        }
    }

    private fun openPreviewIfReady() {
        val surfaceHolder = holder ?: return
        if (!started) return
        session.open(surfaceHolder)
    }
}
