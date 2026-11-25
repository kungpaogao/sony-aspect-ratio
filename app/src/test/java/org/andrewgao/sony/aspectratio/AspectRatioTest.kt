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
}
