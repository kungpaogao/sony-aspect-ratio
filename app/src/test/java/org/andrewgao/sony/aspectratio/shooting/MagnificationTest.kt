package org.andrewgao.sony.aspectratio.shooting

import org.andrewgao.sony.aspectratio.CropRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MagnificationTest {
    @Test
    fun labelIsWholePartThenOneDecimal() {
        assertEquals("×5.9", Magnification(on = true, ratio = 590).label)
        assertEquals("×11.7", Magnification(on = true, ratio = 1170).label)
        assertEquals("×1.0", Magnification(on = true, ratio = 100).label)
    }

    @Test
    fun labelIsEmptyWhenOff() {
        assertEquals("", Magnification().label)
        assertEquals("", Magnification(on = false, ratio = 590).label)
    }

    @Test
    fun cycleGoesOffThroughEveryLevelAndBackToOff() {
        val levels = listOf(2, 3)
        assertEquals(2, nextLevel(levels, null))
        assertEquals(3, nextLevel(levels, 2))
        assertNull(nextLevel(levels, 3))
    }

    @Test
    fun cycleWithOneLevelTogglesOnAndOff() {
        assertEquals(2, nextLevel(listOf(2), null))
        assertNull(nextLevel(listOf(2), 2))
    }

    @Test
    fun cycleRestartsWhenCurrentIsNotInTheList() {
        assertEquals(2, nextLevel(listOf(2, 3), 9))
    }

    @Test
    fun cycleWithNoLevelsStaysOff() {
        assertNull(nextLevel(emptyList(), null))
        assertNull(nextLevel(emptyList(), 2))
    }

    @Test
    fun stepGridKeepsTheFrameEdgeInsideTheRange() {
        // 590 → a 339-unit frame, so the centre can go as far as 830.5, rounded to 831, and the
        // frame edge stays at 1000.
        val grid = MagnifierGrid(590)
        val (x, y) = grid.toCoordinate(12, -12)
        assertEquals(831, x)
        assertEquals(-831, y)
        assertTrue(x + 339 / 2 <= 1000 + 1)
    }

    @Test
    fun theCenterIsStepZero() {
        val grid = MagnifierGrid(590)
        assertEquals(Pair(0, 0), grid.toStep(0, 0))
        assertEquals(Pair(0, 0), grid.toCoordinate(0, 0))
    }

    @Test
    fun toStepRoundsToTheNearestStepAndClamps() {
        val grid = MagnifierGrid(590)
        assertEquals(Pair(1, -1), grid.toStep(60, -80))
        assertEquals(Pair(0, 0), grid.toStep(30, -30))
        assertEquals(Pair(12, -12), grid.toStep(1000, -1000))
    }

    @Test
    fun toCoordinateClampsBeforeMultiplying() {
        val grid = MagnifierGrid(590)
        assertEquals(grid.toCoordinate(12, 12), grid.toCoordinate(50, 50))
        assertEquals(grid.toCoordinate(-12, -12), grid.toCoordinate(-50, -50))
    }

    @Test
    fun movedGoesOneStepFromTheNearestStep() {
        val grid = MagnifierGrid(590)
        val one = grid.toCoordinate(1, 0)
        assertEquals(one, grid.moved(0, 0, 1, 0))
        assertEquals(grid.toCoordinate(-1, 1), grid.moved(0, 0, -1, 1))
        // Off-grid positions snap to the grid first.
        assertEquals(grid.toCoordinate(2, 0), grid.moved(one.first + 10, 0, 1, 0))
    }

    @Test
    fun movedStopsAtTheEdge() {
        val grid = MagnifierGrid(590)
        val edge = grid.toCoordinate(12, 12)
        assertEquals(edge, grid.moved(edge.first, edge.second, 1, 1))
    }

    @Test
    fun aRatioOfZeroTreatsTheFrameAsZero() {
        val grid = MagnifierGrid(0)
        assertEquals(Pair(1000, -1000), grid.toCoordinate(12, -12))
    }

    @Test
    fun aFullFrameHasNowhereToMove() {
        // 100 → the frame is the whole 2000 range, so every step is 0 units.
        val grid = MagnifierGrid(100)
        assertEquals(Pair(0, 0), grid.moved(0, 0, 1, 1))
        assertEquals(Pair(0, 0), grid.toStep(500, 500))
    }

    @Test
    fun zoneIsCenteredWhenTheCenterIsZero() {
        // x7.2 of a 720x480 frame is 100x66, around (360, 240).
        val zone = Magnification(on = true, ratio = 720).zoneIn(720, 480)
        assertEquals(CropRect(310, 207, 100, 66), zone)
    }

    @Test
    fun zoneFollowsTheCenter() {
        val zone = Magnification(on = true, ratio = 1000, x = 500, y = -500).zoneIn(1000, 800)
        // Center (750, 200), size 100x80.
        assertEquals(CropRect(700, 160, 100, 80), zone)
    }

    @Test
    fun zoneIsClampedInsideTheFrameAtTheCorners() {
        val topLeft = Magnification(on = true, ratio = 1000, x = -1000, y = -1000)
        assertEquals(CropRect(0, 0, 100, 80), topLeft.zoneIn(1000, 800))
        val bottomRight = Magnification(on = true, ratio = 1000, x = 1000, y = 1000)
        assertEquals(CropRect(900, 720, 100, 80), bottomRight.zoneIn(1000, 800))
    }

    @Test
    fun zoneIsAtLeastOnePixel() {
        val zone = Magnification(on = true, ratio = 100000).zoneIn(96, 64)!!
        assertEquals(1, zone.width)
        assertEquals(1, zone.height)
    }

    @Test
    fun zoneIsNullWhenOffOrRatioIsNotPositive() {
        assertNull(Magnification().zoneIn(96, 64))
        assertNull(Magnification(on = false, ratio = 720).zoneIn(96, 64))
        assertNull(Magnification(on = true, ratio = 0).zoneIn(96, 64))
    }
}
