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
    private val outerBlack = Paint()
    private val whiteBand = Paint()
    private var aspect = AspectRatio.DEFAULT
    private var liveView: CropRect? = null

    init {
        dim.color = 0xBD6E6E6E.toInt()
        dim.style = Paint.Style.FILL
        outerBlack.color = Color.BLACK
        outerBlack.style = Paint.Style.STROKE
        outerBlack.strokeWidth = 1f
        whiteBand.color = Color.WHITE
        whiteBand.style = Paint.Style.STROKE
        whiteBand.strokeWidth = 4f
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
        val halfOuter = outerBlack.strokeWidth / 2f
        val outer =
            RectF(
                frame.left + halfOuter,
                frame.top + halfOuter,
                frame.right - halfOuter,
                frame.bottom - halfOuter,
            )
        canvas.drawRect(outer, outerBlack)
        val insetWhite = halfOuter + outerBlack.strokeWidth / 2f + whiteBand.strokeWidth / 2f
        val white =
            RectF(
                frame.left + insetWhite,
                frame.top + insetWhite,
                frame.right - insetWhite,
                frame.bottom - insetWhite,
            )
        canvas.drawRect(white, whiteBand)
    }
}
