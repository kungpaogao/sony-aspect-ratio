package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import android.graphics.Rect
import com.github.ma1co.openmemories.framework.DisplayManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.andrewgao.sony.aspectratio.CropRect

/** Live-view rectangle on the frame buffer, from the framework DisplayManager. */
class LiveViewArea {
    private val mutableArea = MutableStateFlow<CropRect?>(null)
    val area: StateFlow<CropRect?> = mutableArea

    private var displayManager: DisplayManager? = null

    fun start(context: Context) {
        try {
            val manager = DisplayManager.create(context)
            displayManager = manager
            publish(manager.videoRect)
            manager.setVideoRectListener { rect -> publish(rect) }
        } catch (error: Exception) {
            Trace.line("live view failed " + error.message)
            mutableArea.value = null
        }
    }

    fun stop() {
        // release() also drops the rect listener.
        displayManager?.release()
        displayManager = null
    }

    private fun publish(rect: Rect) {
        val crop = rect.toCropRect()
        Trace.line("live view $crop")
        mutableArea.value = crop
    }
}

private fun Rect.toCropRect(): CropRect = CropRect(left, top, right - left, bottom - top)
