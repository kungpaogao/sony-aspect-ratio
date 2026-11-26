package org.andrewgao.sony.aspectratio.shooting

import org.junit.Assert.assertEquals
import org.junit.Test

class FocusModeTest {
    @Test
    fun autoFocusModeDecidesWhenFocusIsAuto() {
        assertEquals(FocusMode.AF_S, FocusMode.from("auto", "af-s"))
        assertEquals(FocusMode.AF_C, FocusMode.from("auto", "af-c"))
        assertEquals(FocusMode.AF_A, FocusMode.from("auto", "af-a"))
    }

    @Test
    fun movieAutoFocusShowsAsAfC() {
        assertEquals(FocusMode.AF_C, FocusMode.from("auto", "movie"))
    }

    @Test
    fun manualAndDmfIgnoreTheAutoFocusMode() {
        assertEquals(FocusMode.MF, FocusMode.from("manual", null))
        assertEquals(FocusMode.MF, FocusMode.from("manual", "af-s"))
        assertEquals(FocusMode.DMF, FocusMode.from("dmf", null))
        assertEquals(FocusMode.DMF, FocusMode.from("dmf", "af-c"))
    }

    @Test
    fun unknownAutoFocusModeIsUnknown() {
        assertEquals(FocusMode.UNKNOWN, FocusMode.from("auto", null))
        assertEquals(FocusMode.UNKNOWN, FocusMode.from("auto", "auto"))
    }

    @Test
    fun unknownFocusModeIsUnknown() {
        assertEquals(FocusMode.UNKNOWN, FocusMode.from(null, null))
        assertEquals(FocusMode.UNKNOWN, FocusMode.from("macro", "af-s"))
    }

    @Test
    fun unknownHasNoLabel() {
        assertEquals("", FocusMode.UNKNOWN.label)
    }
}
