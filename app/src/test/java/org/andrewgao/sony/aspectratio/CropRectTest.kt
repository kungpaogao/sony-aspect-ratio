package org.andrewgao.sony.aspectratio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CropRectTest {
    // ILCE-7R full-size still.
    private val width = 7360
    private val height = 4912

    @Test
    fun xpanOnFourByThreeUsesTheFullWidth() {
        val crop = CropRect.centered(640, 480, AspectRatio(65, 24))
        assertEquals(0, crop.left)
        assertEquals(640, crop.width)
        assertEquals(236, crop.height)
        assertEquals(122, crop.top)
    }

    @Test
    fun squareOnThreeByTwoCropsTheSides() {
        val crop = CropRect.centered(3000, 2000, AspectRatio(1, 1))
        assertEquals(2000, crop.width)
        assertEquals(2000, crop.height)
        assertEquals(500, crop.left)
        assertEquals(0, crop.top)
    }

    @Test
    fun xpanOnTheA7rKeepsTheWidthAndSnapsTheHeight() {
        val crop = CropRect.aligned(width, height, AspectRatio(65, 24))
        assertEquals(0, crop.left)
        assertEquals(7360, crop.width)
        assertEquals(2712, crop.height)
        assertEquals(1096, crop.top)
    }

    @Test
    fun everyRatioIsAlignedAndInsideTheFrame() {
        for (aspect in AspectRatio.RATIOS) {
            val crop = CropRect.aligned(width, height, aspect)
            assertEquals(aspect.label, 0, crop.left % CropRect.ALIGN)
            assertEquals(aspect.label, 0, crop.top % CropRect.ALIGN)
            assertEquals(aspect.label, 0, crop.width % CropRect.ALIGN)
            assertEquals(aspect.label, 0, crop.height % CropRect.ALIGN)
            assertTrue(aspect.label, crop.left + crop.width <= width)
            assertTrue(aspect.label, crop.top + crop.height <= height)
            val wanted = aspect.width.toDouble() / aspect.height
            val got = crop.width.toDouble() / crop.height
            assertTrue(aspect.label + " ratio " + got, Math.abs(got - wanted) / wanted < 0.01)
        }
    }

    @Test
    fun threeByTwoKeepsTheNativeFrame() {
        assertEquals(CropRect(0, 0, width, height), CropRect.aligned(width, height, AspectRatio(3, 2)))
    }

    @Test
    fun aTinyFrameFallsBackToTheWholeImage() {
        val crop = CropRect.aligned(6, 6, AspectRatio(1, 1))
        assertEquals(CropRect(0, 0, 6, 6), crop)
    }
}
