package org.andrewgao.sony.aspectratio

import android.graphics.Rect

/** A crop in pixels: top-left corner and size. */
data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int) {
    val right: Int
        get() = left + width

    val bottom: Int
        get() = top + height

    fun isWholeFrame(frameWidth: Int, frameHeight: Int): Boolean =
        left == 0 && top == 0 && width == frameWidth && height == frameHeight

    fun toRect(): Rect = Rect(left, top, right, bottom)

    companion object {
        /** Hardware crops and the JPEG encoder want block-aligned sizes. */
        const val ALIGN = 8

        /** Largest centered rectangle of [aspect] that fits inside the frame. */
        fun centered(frameWidth: Int, frameHeight: Int, aspect: AspectRatio): CropRect {
            val frameIsWider =
                frameWidth.toLong() * aspect.height > frameHeight.toLong() * aspect.width
            val width =
                if (frameIsWider) (frameHeight.toLong() * aspect.width / aspect.height).toInt()
                else frameWidth
            val height =
                if (frameIsWider) frameHeight
                else (frameWidth.toLong() * aspect.height / aspect.width).toInt()
            return CropRect((frameWidth - width) / 2, (frameHeight - height) / 2, width, height)
        }

        /** [centered] inside [area], offset by [area]'s top-left corner. */
        fun centeredIn(area: CropRect, aspect: AspectRatio): CropRect {
            val inner = centered(area.width, area.height, aspect)
            return CropRect(area.left + inner.left, area.top + inner.top, inner.width, inner.height)
        }

        /**
         * [centered], snapped down to [ALIGN] so the hardware crop and JPEG encoder behind
         * ImageFilters.crop and JpegSaver accept it. A crop within one block of the frame (3:2 on a
         * 3:2 sensor) keeps the whole frame.
         */
        fun aligned(frameWidth: Int, frameHeight: Int, aspect: AspectRatio): CropRect {
            val crop = centered(frameWidth, frameHeight, aspect)
            val whole = CropRect(0, 0, frameWidth, frameHeight)
            if (crop.width > frameWidth - ALIGN && crop.height > frameHeight - ALIGN) return whole
            val mask = (ALIGN - 1).inv()
            val cropWidth = crop.width and mask
            val cropHeight = crop.height and mask
            if (cropWidth <= 0 || cropHeight <= 0) return whole
            val left = ((frameWidth - cropWidth) / 2) and mask
            val top = ((frameHeight - cropHeight) / 2) and mask
            return CropRect(left, top, cropWidth, cropHeight)
        }
    }
}
