package org.andrewgao.sony.aspectratio

/**
 * Sony's setAspectRatio only accepts "3:2", "16:9", "4:3", and "1:1". 65:24, 17:6, and 5:4 are
 * outside that set, so the saved JPEG is cropped in the app.
 */
data class AspectRatio(val width: Int, val height: Int) {
    val label: String
        get() = "$width:$height"

    companion object {
        val RATIOS =
            listOf(
                AspectRatio(3, 2),
                AspectRatio(16, 9),
                AspectRatio(65, 24),
                AspectRatio(17, 6),
                AspectRatio(1, 1),
                AspectRatio(5, 4),
            )

        val DEFAULT = RATIOS[2]

        fun step(current: AspectRatio, delta: Int): AspectRatio =
            RATIOS[(RATIOS.indexOf(current) + delta).mod(RATIOS.size)]
    }
}
