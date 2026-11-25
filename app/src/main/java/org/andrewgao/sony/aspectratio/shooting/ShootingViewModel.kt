package org.andrewgao.sony.aspectratio.shooting

import android.content.Context
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.CameraKeys
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.camera.CropSession
import org.andrewgao.sony.aspectratio.camera.LiveViewArea
import org.andrewgao.sony.aspectratio.camera.Trace
import org.andrewgao.sony.aspectratio.shared.ViewModel

class ShootingViewModel(
    private val context: Context,
    recreateSurface: () -> Unit,
    initialMessage: String = "",
) : ViewModel<ShootingState>(ShootingState(message = initialMessage)) {
    private val session = CropSession.create(recreateSurface = recreateSurface, onStatus = ::status)
    private val liveViewArea = LiveViewArea()
    private var shot: Job? = null
    private var holder: SurfaceHolder? = null
    private var started = false

    init {
        if (initialMessage.isNotEmpty()) Trace.line(initialMessage)
        scope.launch {
            liveViewArea.area.collect { area -> setState { it.copy(liveView = area) } }
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
        openPreviewIfReady()
    }

    override fun onStop() {
        started = false
        liveViewArea.stop()
        shot?.cancel()
        shot = null
        session.release()
    }

    fun onKeyDown(key: Int): Boolean {
        if (key == CameraKeys.ENTER) {
            setState { it.copy(choosingRatio = !it.choosingRatio) }
            return true
        }
        val dial = if (state.value.choosingRatio) dialStep(key) else 0
        if (dial != 0) {
            setState {
                it.copy(aspect = AspectRatio.step(it.aspect, dial))
            }
            return true
        }
        return false
    }

    fun shoot() {
        if (shot?.isActive == true) return
        val aspect = state.value.aspect
        status("shutter " + aspect.label)
        shot = scope.launch {
            setState { it.copy(shooting = true) }
            try {
                status(session.takePicture(context, aspect))
            } finally {
                shot = null
                setState { it.copy(shooting = false) }
            }
        }
    }

    private fun openPreviewIfReady() {
        val surfaceHolder = holder ?: return
        if (!started) return
        session.open(surfaceHolder)
    }

    private fun status(message: String) {
        Trace.line(message)
        setState { it.copy(message = message) }
    }
}
