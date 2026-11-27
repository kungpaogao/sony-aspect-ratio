package org.andrewgao.sony.aspectratio.shooting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExposureStepsTest {
    private fun label(s: ShutterSpeed) = ExposureFormat.shutter(s.numerator, s.denominator)

    @Test
    fun shutterThirdsFormatToTheStandardLabels() {
        val expected =
            "30\" 25\" 20\" 15\" 13\" 10\" 8\" 6\" 5\" 4\" 3.2\" 2.5\" 2\" 1.6\" 1.3\" 1\" 0.8\" " +
                "0.6\" 0.5\" 0.4\" 1/3 1/4 1/5 1/6 1/8 1/10 1/13 1/15 1/20 1/25 1/30 1/40 1/50 " +
                "1/60 1/80 1/100 1/125 1/160 1/200 1/250 1/320 1/400 1/500 1/640 1/800 1/1000 " +
                "1/1250 1/1600 1/2000 1/2500 1/3200 1/4000 1/5000 1/6400 1/8000"
        assertEquals(expected.split(' ').size, ExposureSteps.SHUTTER_THIRDS.size)
        assertEquals(expected, ExposureSteps.SHUTTER_THIRDS.joinToString(" ") { label(it) })
    }

    @Test
    fun shutterHalvesFormat() {
        val labels = ExposureSteps.SHUTTER_HALVES.joinToString(" ") { label(it) }
        assertTrue(
            labels,
            labels.startsWith("30\" 20\" 15\" 10\" 8\" 6\" 4\" 3\" 2\" 1.5\" 1\" 0.7\" 0.5\" 1/3"),
        )
    }

    @Test
    fun tablesAreStrictlyOrdered() {
        for (table in listOf(ExposureSteps.SHUTTER_THIRDS, ExposureSteps.SHUTTER_HALVES)) {
            assertTrue(table.zipWithNext().all { (a, b) -> a.seconds > b.seconds })
        }
        for (table in listOf(ExposureSteps.APERTURE_THIRDS, ExposureSteps.APERTURE_HALVES)) {
            assertTrue(table.zipWithNext().all { (a, b) -> a < b })
        }
    }

    @Test
    fun sonyPairsFindTheirNearestStep() {
        // 10/16 is 0.625 s: the 0.6" step; one faster step is 0.5".
        assertEquals("0.5\"", label(ExposureSteps.nextShutter(10, 16, 1, false, null)!!))
        assertEquals("0.8\"", label(ExposureSteps.nextShutter(10, 16, -1, false, null)!!))
        assertEquals("1\"", label(ExposureSteps.nextShutter(13, 10, 1, false, null)!!))
        assertEquals("1/80", label(ExposureSteps.nextShutter(1, 60, 1, false, null)!!))
        assertEquals("1/50", label(ExposureSteps.nextShutter(1, 60, -1, false, null)!!))
    }

    @Test
    fun shutterStepsSeveralAtOnceAndClampsAtTheTable() {
        assertEquals("1/100", label(ExposureSteps.nextShutter(1, 60, 2, false, null)!!))
        assertEquals("1/8000", label(ExposureSteps.nextShutter(1, 8000, 3, false, null)!!))
        assertEquals("30\"", label(ExposureSteps.nextShutter(30, 1, -3, false, null)!!))
    }

    @Test
    fun shutterHalfStopStepsAreWider() {
        assertEquals("1/90", label(ExposureSteps.nextShutter(1, 60, 1, true, null)!!))
    }

    @Test
    fun shutterClampsToTheRangeWhicheverEndIsNamedFirst() {
        val range = intArrayOf(1, 250, 1, 4000)
        assertEquals("1/250", label(ExposureSteps.nextShutter(1, 250, -1, false, range)!!))
        assertEquals("1/4000", label(ExposureSteps.nextShutter(1, 4000, 1, false, range)!!))
        val swapped = intArrayOf(1, 4000, 1, 250)
        assertEquals("1/250", label(ExposureSteps.nextShutter(1, 250, -1, false, swapped)!!))
    }

    @Test
    fun shutterIgnoresBulbAndUnknown() {
        assertNull(ExposureSteps.nextShutter(65535, 0, 1, false, null))
        assertNull(ExposureSteps.nextShutter(0, 0, 1, false, null))
    }

    @Test
    fun apertureThirdsAndHalves() {
        assertEquals(320, ExposureSteps.nextAperture(280, 1, false, null))
        assertEquals(250, ExposureSteps.nextAperture(280, -1, false, null))
        assertEquals(330, ExposureSteps.nextAperture(280, 1, true, null))
        // A reported value off the table still lands on the nearest step.
        assertEquals(400, ExposureSteps.nextAperture(356, 1, false, null))
    }

    @Test
    fun apertureClampsToTheRangeAndTable() {
        val range = intArrayOf(400, 800)
        assertEquals(400, ExposureSteps.nextAperture(400, -1, false, range))
        assertEquals(800, ExposureSteps.nextAperture(800, 1, false, range))
        assertEquals(450, ExposureSteps.nextAperture(400, 1, false, range))
        assertEquals(6400, ExposureSteps.nextAperture(6400, 1, false, null))
        assertNull(ExposureSteps.nextAperture(0, 1, false, null))
    }

    @Test
    fun apertureLabelsAreStandard() {
        val labels = ExposureSteps.APERTURE_THIRDS.joinToString(" ") { ExposureFormat.aperture(it) }
        assertTrue(labels, labels.startsWith("F1.0 F1.1 F1.2 F1.4 F1.6 F1.8 F2.0 F2.2 F2.5 F2.8"))
        assertTrue(labels, labels.contains("F9.0 F10 F11 F13 F14 F16"))
    }

    @Test
    fun halfStopDetection() {
        assertTrue(ExposureSteps.isHalfStop(0.5f))
        assertFalse(ExposureSteps.isHalfStop(0.333f))
        assertFalse(ExposureSteps.isHalfStop(0f))
    }

    @Test
    fun isoStepsKeepTheCamerasOrder() {
        val list = listOf(0, 100, 200, 400)
        assertEquals(200, IsoSteps.next(list, 100, 1))
        assertEquals(0, IsoSteps.next(list, 100, -1))
        assertNull(IsoSteps.next(list, 400, 1))
        assertEquals(400, IsoSteps.next(list, 100, 5))
        assertNull(IsoSteps.next(emptyList(), 100, 1))
        // Off-list current value snaps to the nearest entry first.
        assertEquals(400, IsoSteps.next(list, 250, 1))
    }
}
