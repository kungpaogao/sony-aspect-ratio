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
        if (isFocusKey(key)) {
            session.controls.focus()
            return true
        }
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

    /**
     * Back-button focus: the AEL button holds focus and the shutter shoots with it; half-press does
     * nothing. The a7R's AEL button arrives as 638 (AF_MF_AEL); 532 and 533 cover the other lever
     * positions.
     */
    private fun isFocusKey(key: Int): Boolean =
        key == CameraKeys.AF_MF_AEL || key == CameraKeys.AEL || key == CameraKeys.AF_MF

    fun onKeyUp(key: Int): Boolean {
        if (isFocusKey(key)) {
            session.controls.cancelFocus()
            return true
        }
        return false
    }

    fun shoot() {
        if (shot?.isActive == true) return
        val aspect = state.value.aspect
        Trace.line("shutter " + aspect.label)
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
