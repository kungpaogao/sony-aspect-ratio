package org.andrewgao.sony.aspectratio

/**
 * Sony's setAspectRatio only accepts "3:2", "16:9", "4:3", and "1:1".
 * 65:24, 17:6, and 5:4 are outside that set, so the saved JPEG is cropped in the app.
 */
class AspectRatio(val width: Int, val height: Int) {
    val label: String
        get() = "$width:$height"

    companion object {
        val RATIOS = listOf(
            AspectRatio(3, 2),
            AspectRatio(16, 9),
            AspectRatio(65, 24),
            AspectRatio(17, 6),
            AspectRatio(1, 1),
            AspectRatio(5, 4)
        )

        val DEFAULT = RATIOS[2]

        fun step(current: AspectRatio, delta: Int): AspectRatio {
            if (RATIOS.isEmpty()) return current
            val index = RATIOS.indexOfFirst { it.width == current.width && it.height == current.height }
            val start = if (index < 0) 0 else index
            val size = RATIOS.size
            val moved = delta % size
            val next = (start + moved + size) % size
            return RATIOS[next]
        }
    }
}

data class CropRect(val left: Int, val top: Int, val width: Int, val height: Int)

/** Largest centered rectangle of [aspect] that fits inside the frame. */
fun centeredCrop(frameWidth: Int, frameHeight: Int, aspect: AspectRatio): CropRect {
    if (frameWidth <= 1 || frameHeight <= 1 || aspect.width <= 0 || aspect.height <= 0) {
        return CropRect(0, 0, 0, 0)
    }
    val frameIsWider = frameWidth.toLong() * aspect.height > frameHeight.toLong() * aspect.width
    var cropWidth: Int
    var cropHeight: Int
    if (frameIsWider) {
        cropHeight = frameHeight
        cropWidth = (frameHeight.toLong() * aspect.width / aspect.height).toInt()
    } else {
        cropWidth = frameWidth
        cropHeight = (frameWidth.toLong() * aspect.height / aspect.width).toInt()
    }
    if (cropWidth > frameWidth) cropWidth = frameWidth
    if (cropHeight > frameHeight) cropHeight = frameHeight
    cropWidth = even(cropWidth)
    cropHeight = even(cropHeight)
    if (cropWidth < 2) cropWidth = even(frameWidth.coerceAtMost(2).coerceAtLeast(0))
    if (cropHeight < 2) cropHeight = even(frameHeight.coerceAtMost(2).coerceAtLeast(0))
    var left = even((frameWidth - cropWidth) / 2)
    var top = even((frameHeight - cropHeight) / 2)
    if (left + cropWidth > frameWidth) cropWidth = even(frameWidth - left)
    if (top + cropHeight > frameHeight) cropHeight = even(frameHeight - top)
    if (cropWidth < 2 || cropHeight < 2) return CropRect(0, 0, 0, 0)
    return CropRect(left, top, cropWidth, cropHeight)
}

private fun even(value: Int): Int {
    val cleared = value and 1.inv()
    return if (cleared < 0) 0 else cleared
}
