package org.andrewgao.sony.aspectratio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import org.andrewgao.sony.aspectratio.shooting.Magnification
import org.andrewgao.sony.aspectratio.shooting.zoneIn

/** A small map of the whole frame with a marker on the part the focus magnifier is showing. */
class MagnifierNavigatorView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val background = Paint()
    private val frame = Paint()
    private val marker = Paint()
    private var magnification = Magnification()

    init {
        background.color = BACKGROUND_COLOR
        background.style = Paint.Style.FILL
        frame.color = FRAME_COLOR
        frame.style = Paint.Style.STROKE
        frame.strokeWidth = FRAME_WIDTH
        marker.color = MARKER_COLOR
        marker.style = Paint.Style.STROKE
        marker.strokeWidth = MARKER_WIDTH
        setWillNotDraw(false)
    }

    fun setMagnification(next: Magnification) {
        if (next == magnification) return
        magnification = next
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        canvas.drawRect(0f, 0f, w, h, background)
        // A stroke is centered on its path, so inset half its width to keep the line inside the
        // rect.
        val frameHalf = frame.strokeWidth / 2f
        canvas.drawRect(frameHalf, frameHalf, w - frameHalf, h - frameHalf, frame)
        val zone = magnification.zoneIn(width, height) ?: return
        val markerHalf = marker.strokeWidth / 2f
        canvas.drawRect(
            zone.left + markerHalf,
            zone.top + markerHalf,
            zone.right - markerHalf,
            zone.bottom - markerHalf,
            marker,
        )
    }

    private companion object {
        // Sony's MF assist screen colors, RESID_LINESTYLE_MF_ASSIST_COLOR_* (background, outline,
        // moving marker).
        const val BACKGROUND_COLOR = 0x88000000.toInt()
        const val FRAME_COLOR = 0xFFDDDDDD.toInt()
        const val MARKER_COLOR = 0xFFDD5500.toInt()
        const val FRAME_WIDTH = 2f
        const val MARKER_WIDTH = 3f
    }
}
