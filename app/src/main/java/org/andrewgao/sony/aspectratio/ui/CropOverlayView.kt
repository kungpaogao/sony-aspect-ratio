package org.andrewgao.sony.aspectratio.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

/**
 * Largest 65:24 rectangle that fits the preview, with the rest dimmed.
 * 65:24 is the XPan frame.
 */
class CropOverlayView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    private val dim = Paint()
    private val stroke = Paint()
    private val outline = Paint()
    private val label = Paint()

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

    override fun onDraw(canvas: Canvas) {
        val frame = cropFrame(width.toFloat(), height.toFloat(), CROP_WIDTH, CROP_HEIGHT)
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
        val text = "$CROP_WIDTH:$CROP_HEIGHT"
        val textWidth = label.measureText(text)
        val gap = resources.displayMetrics.density * 6f
        canvas.drawText(
            text,
            frame.left + (frame.width() - textWidth) / 2f,
            frame.top - gap,
            label
        )
    }

    companion object {
        const val CROP_WIDTH = 65
        const val CROP_HEIGHT = 24
    }
}

internal fun cropFrame(width: Float, height: Float, aspectWidth: Int, aspectHeight: Int): RectF {
    if (width <= 0f || height <= 0f || aspectWidth <= 0 || aspectHeight <= 0) {
        return RectF()
    }
    val target = aspectWidth.toFloat() / aspectHeight.toFloat()
    val view = width / height
    val cropWidth: Float
    val cropHeight: Float
    if (view > target) {
        cropHeight = height
        cropWidth = height * target
    } else {
        cropWidth = width
        cropHeight = width / target
    }
    val left = (width - cropWidth) / 2f
    val top = (height - cropHeight) / 2f
    return RectF(left, top, left + cropWidth, top + cropHeight)
}
