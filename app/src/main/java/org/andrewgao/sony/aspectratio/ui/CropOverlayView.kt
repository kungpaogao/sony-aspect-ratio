package org.andrewgao.sony.aspectratio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.CropRect

/** Largest rectangle of the selected aspect that fits the preview, with the rest dimmed. */
class CropOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val dim = Paint()
    private val border = Paint()
    private var aspect = AspectRatio.DEFAULT
    private var liveView: CropRect? = null
    private var focusLocked = false

    init {
        dim.color = 0x40000000 // black at 25%
        dim.style = Paint.Style.FILL
        border.color = BORDER_COLOR
        border.style = Paint.Style.STROKE
        border.strokeWidth = BORDER_WIDTH
        setWillNotDraw(false)
    }

    fun setAspect(next: AspectRatio) {
        aspect = next
        invalidate()
    }

    fun setLiveView(area: CropRect?) {
        liveView = area
        invalidate()
    }

    fun setFocusLocked(locked: Boolean) {
        if (locked == focusLocked) return
        focusLocked = locked
        border.color = if (locked) LOCKED_BORDER_COLOR else BORDER_COLOR
        border.strokeWidth = if (locked) LOCKED_BORDER_WIDTH else BORDER_WIDTH
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val bounds = liveView ?: CropRect(0, 0, width, height)
        val crop = CropRect.centeredIn(bounds, aspect)
        val frame =
            RectF(
                crop.left.toFloat(),
                crop.top.toFloat(),
                crop.right.toFloat(),
                crop.bottom.toFloat(),
            )
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top, dim)
        canvas.drawRect(0f, frame.bottom, width.toFloat(), height.toFloat(), dim)
        canvas.drawRect(0f, frame.top, frame.left, frame.bottom, dim)
        canvas.drawRect(frame.right, frame.top, width.toFloat(), frame.bottom, dim)
        // A stroke is centered on its path, so inset half its width to keep the line inside the
        // crop.
        val half = border.strokeWidth / 2f
        canvas.drawRect(
            frame.left + half,
            frame.top + half,
            frame.right - half,
            frame.bottom - half,
            border,
        )
    }

    private companion object {
        const val BORDER_COLOR = Color.WHITE
        const val BORDER_WIDTH = 1f
        // Paper artboard "AF on": green 4px frame while autofocus is locked.
        const val LOCKED_BORDER_COLOR = 0xFF02C300.toInt()
        const val LOCKED_BORDER_WIDTH = 4f
    }
}
