package org.andrewgao.sony.aspectratio

import org.junit.Assert.assertEquals
import org.junit.Test

class CenteredCropTest {
    @Test
    fun xpanOnFourByThreeUsesTheFullWidth() {
        val crop = centeredCrop(640, 480, AspectRatio(65, 24))
        assertEquals(0, crop.left)
        assertEquals(640, crop.width)
        assertEquals(236, crop.height)
        assertEquals(122, crop.top)
    }

    @Test
    fun squareOnThreeByTwoCropsTheSides() {
        val crop = centeredCrop(3000, 2000, AspectRatio(1, 1))
        assertEquals(2000, crop.width)
        assertEquals(2000, crop.height)
        assertEquals(500, crop.left)
        assertEquals(0, crop.top)
    }

    @Test
    fun dialStepsThroughTheRatioList() {
        val first = AspectRatio.RATIOS[0]
        assertEquals(AspectRatio.RATIOS[1].label, AspectRatio.step(first, 1).label)
        assertEquals(AspectRatio.RATIOS.last().label, AspectRatio.step(first, -1).label)
    }
}
