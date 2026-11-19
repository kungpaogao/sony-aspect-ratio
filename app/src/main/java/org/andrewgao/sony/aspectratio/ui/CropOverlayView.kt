package org.andrewgao.sony.aspectratio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.centeredCrop

/**
 * Largest rectangle of the selected aspect that fits the preview, with the rest dimmed.
 */
class CropOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val dim = Paint()
    private val stroke = Paint()
    private val outline = Paint()
    private val label = Paint()
    private var aspect = AspectRatio.DEFAULT

    init {
        dim.color = 0x99000000.toInt()
        dim.style = Paint.Style.FILL
        val density = resources.displayMetrics.density
        stroke.color = Color.WHITE
        stroke.style = Paint.Style.STROKE
        stroke.strokeWidth = density * 2f
        outline.color = Color.BLACK
        outline.style = Paint.Style.STROKE
        outline.strokeWidth = density * 4f
        label.color = Color.WHITE
        label.textSize = resources.displayMetrics.density * 14f
        label.isAntiAlias = true
        label.setShadowLayer(2f, 0f, 1f, Color.BLACK)
        setWillNotDraw(false)
    }

    fun setAspect(next: AspectRatio, choosing: Boolean) {
        aspect = next
        label.color = if (choosing) Color.YELLOW else Color.WHITE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        val crop = centeredCrop(width, height, aspect)
        val frame = RectF(
            crop.left.toFloat(),
            crop.top.toFloat(),
            (crop.left + crop.width).toFloat(),
            (crop.top + crop.height).toFloat()
        )
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top, dim)
        canvas.drawRect(0f, frame.bottom, width.toFloat(), height.toFloat(), dim)
        canvas.drawRect(0f, frame.top, frame.left, frame.bottom, dim)
        canvas.drawRect(frame.right, frame.top, width.toFloat(), frame.bottom, dim)
        val half = outline.strokeWidth / 2f
        val box = RectF(
            frame.left + half,
            frame.top + half,
            frame.right - half,
            frame.bottom - half
        )
        canvas.drawRect(box, outline)
        canvas.drawRect(box, stroke)
        val text = aspect.label
        val textWidth = label.measureText(text)
        val gap = resources.displayMetrics.density * 6f
        canvas.drawText(
            text,
            frame.left + (frame.width() - textWidth) / 2f,
            frame.top - gap,
            label
        )
    }
}
