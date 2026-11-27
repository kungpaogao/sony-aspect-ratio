package org.andrewgao.sony.aspectratio.shooting

import org.junit.Assert.assertEquals
import org.junit.Test

class OrientationTest {
    @Test
    fun mapsPortraitAndUpsideDownExifToDegrees() {
        assertEquals(90, rotationDegrees(6))
        assertEquals(180, rotationDegrees(3))
        assertEquals(-90, rotationDegrees(8))
    }

    @Test
    fun uprightAndUnknownExifNeedNoRotation() {
        assertEquals(0, rotationDegrees(1))
        assertEquals(0, rotationDegrees(0))
        assertEquals(0, rotationDegrees(5))
    }
}
