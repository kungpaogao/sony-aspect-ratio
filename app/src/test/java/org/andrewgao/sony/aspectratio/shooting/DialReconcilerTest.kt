package org.andrewgao.sony.aspectratio.shooting

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DialReconcilerTest {
    @Test
    fun reportsPassThroughWhenNotStepping() {
        val dial = DialReconciler<Int>()
        assertEquals(5, dial.report(5, 0))
        assertEquals(5, dial.shown)
    }

    @Test
    fun intermediateReportsAreIgnoredDuringTheWindow() {
        val dial = DialReconciler<Int>()
        dial.report(1, 0)
        dial.step(4, 1000)
        assertEquals(4, dial.report(2, 1100))
        assertEquals(4, dial.report(3, 1300))
        assertEquals(4, dial.shown)
    }

    @Test
    fun aMatchingReportIsAccepted() {
        val dial = DialReconciler<Int>()
        dial.step(4, 1000)
        assertEquals(4, dial.report(4, 1100))
    }

    @Test
    fun theCamerasLatestValueWinsAfterTheWindow() {
        val dial = DialReconciler<Int>()
        dial.step(4, 1000)
        dial.report(2, 1100)
        dial.report(3, 1300)
        assertFalse(dial.settle(1300))
        assertTrue(dial.settle(1400))
        assertEquals(3, dial.shown)
    }

    @Test
    fun aLateReportAfterTheWindowIsApplied() {
        val dial = DialReconciler<Int>()
        dial.step(4, 1000)
        assertEquals(7, dial.report(7, 1500))
        assertEquals(7, dial.shown)
        assertTrue(dial.settle(1500))
    }

    @Test
    fun aNewStepExtendsTheWindow() {
        val dial = DialReconciler<Int>()
        dial.step(4, 1000)
        dial.step(5, 1300)
        assertEquals(5, dial.report(4, 1500))
        assertFalse(dial.settle(1500))
    }

    @Test
    fun waitsOnceForASilentCameraThenFallsBackToItsLastValue() {
        val dial = DialReconciler<Int>()
        dial.report(9, 0)
        dial.step(12, 1000)
        assertFalse(dial.settle(1400))
        assertTrue(dial.settle(1800))
        assertEquals(9, dial.shown)
    }
}
