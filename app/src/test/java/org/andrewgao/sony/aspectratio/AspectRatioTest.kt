package org.andrewgao.sony.aspectratio

import org.junit.Assert.assertEquals
import org.junit.Test

class AspectRatioTest {
    @Test
    fun dialStepsThroughTheRatioList() {
        val first = AspectRatio.RATIOS[0]
        assertEquals(AspectRatio.RATIOS[1].label, AspectRatio.step(first, 1).label)
        assertEquals(AspectRatio.RATIOS.last().label, AspectRatio.step(first, -1).label)
    }

    @Test
    fun ratiosAreInDialOrder() {
        assertEquals(
            listOf("3:2", "16:9", "65:24", "17:6", "4:3", "5:4", "1:1"),
            AspectRatio.RATIOS.map { it.label },
        )
    }

    @Test
    fun defaultIsXpan() {
        assertEquals("65:24", AspectRatio.DEFAULT.label)
    }
}
