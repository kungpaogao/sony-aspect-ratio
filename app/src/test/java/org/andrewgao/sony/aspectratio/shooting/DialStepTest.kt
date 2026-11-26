package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys
import org.junit.Assert.assertEquals
import org.junit.Test

class DialStepTest {
    @Test
    fun dialsAndRingStepTheAspect() {
        assertEquals(1, dialStep(CameraKeys.DIAL_1_CLOCKWISE))
        assertEquals(1, dialStep(CameraKeys.DIAL_2_CLOCKWISE))
        assertEquals(1, dialStep(CameraKeys.WHEEL_CLOCKWISE))
        assertEquals(1, dialStep(CameraKeys.DIAL_3_CLOCKWISE))
        assertEquals(1, dialStep(CameraKeys.RING_CLOCKWISE))
        assertEquals(-1, dialStep(CameraKeys.DIAL_1_COUNTERCLOCKWISE))
        assertEquals(-1, dialStep(CameraKeys.DIAL_2_COUNTERCLOCKWISE))
        assertEquals(-1, dialStep(CameraKeys.WHEEL_COUNTERCLOCKWISE))
        assertEquals(-1, dialStep(CameraKeys.DIAL_3_COUNTERCLOCKWISE))
        assertEquals(-1, dialStep(CameraKeys.RING_COUNTERCLOCKWISE))
    }

    @Test
    fun otherKeysDoNotStep() {
        assertEquals(0, dialStep(CameraKeys.ENTER))
        assertEquals(0, dialStep(CameraKeys.SHUTTER))
    }
}
