package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusButtonTest {
    @Test
    fun buttonWithLeverAtAfMfMagnifies() {
        assertEquals(
            FocusButton.MAGNIFY,
            focusButton(CameraKeys.AF_MF_AEL, CameraKeys.AF_MF),
        )
    }

    @Test
    fun buttonWithLeverAtAelFocuses() {
        assertEquals(
            FocusButton.AUTOFOCUS,
            focusButton(CameraKeys.AF_MF_AEL, CameraKeys.AEL),
        )
    }

    @Test
    fun buttonWithUnknownLeverFocuses() {
        assertEquals(FocusButton.AUTOFOCUS, focusButton(CameraKeys.AF_MF_AEL, 0))
        assertEquals(FocusButton.AUTOFOCUS, focusButton(CameraKeys.AF_MF_AEL, -1))
    }

    @Test
    fun directKeysRouteThemselvesWhateverTheLever() {
        for (lever in listOf(0, CameraKeys.AEL, CameraKeys.AF_MF)) {
            assertEquals(FocusButton.AUTOFOCUS, focusButton(CameraKeys.AEL, lever))
            assertEquals(FocusButton.MAGNIFY, focusButton(CameraKeys.AF_MF, lever))
        }
    }

    @Test
    fun otherKeysReturnNull() {
        assertNull(focusButton(CameraKeys.ENTER, CameraKeys.AF_MF))
        assertNull(focusButton(CameraKeys.SLIDE_AEL_FOCUS, CameraKeys.AF_MF))
        assertNull(focusButton(CameraKeys.UP, CameraKeys.AEL))
    }
}
