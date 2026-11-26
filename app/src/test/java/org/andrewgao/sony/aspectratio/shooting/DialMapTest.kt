package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DialMapTest {
    @Test
    fun programMode() {
        assertEquals(
            DialAction.ProgramShift(1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.DIAL_1_CLOCKWISE),
        )
        assertEquals(
            DialAction.ProgramShift(-1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.DIAL_1_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.ProgramShift(1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.DIAL_2_CLOCKWISE),
        )
        assertEquals(
            DialAction.ProgramShift(-1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.DIAL_2_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.WHEEL_CLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(-1),
            dialAction(ExposureMode.PROGRAM, CameraKeys.WHEEL_COUNTERCLOCKWISE),
        )
    }

    @Test
    fun apertureMode() {
        assertEquals(
            DialAction.Aperture(1),
            dialAction(ExposureMode.APERTURE, CameraKeys.DIAL_1_CLOCKWISE),
        )
        assertEquals(
            DialAction.Aperture(-1),
            dialAction(ExposureMode.APERTURE, CameraKeys.DIAL_1_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Aperture(1),
            dialAction(ExposureMode.APERTURE, CameraKeys.DIAL_2_CLOCKWISE),
        )
        assertEquals(
            DialAction.Aperture(-1),
            dialAction(ExposureMode.APERTURE, CameraKeys.DIAL_2_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(1),
            dialAction(ExposureMode.APERTURE, CameraKeys.WHEEL_CLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(-1),
            dialAction(ExposureMode.APERTURE, CameraKeys.WHEEL_COUNTERCLOCKWISE),
        )
    }

    @Test
    fun shutterMode() {
        assertEquals(
            DialAction.Shutter(1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.DIAL_1_CLOCKWISE),
        )
        assertEquals(
            DialAction.Shutter(-1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.DIAL_1_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Shutter(1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.DIAL_2_CLOCKWISE),
        )
        assertEquals(
            DialAction.Shutter(-1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.DIAL_2_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.WHEEL_CLOCKWISE),
        )
        assertEquals(
            DialAction.Iso(-1),
            dialAction(ExposureMode.SHUTTER, CameraKeys.WHEEL_COUNTERCLOCKWISE),
        )
    }

    @Test
    fun manualMode() {
        assertEquals(
            DialAction.Shutter(1),
            dialAction(ExposureMode.MANUAL, CameraKeys.DIAL_1_CLOCKWISE),
        )
        assertEquals(
            DialAction.Shutter(-1),
            dialAction(ExposureMode.MANUAL, CameraKeys.DIAL_1_COUNTERCLOCKWISE),
        )
        assertEquals(
            DialAction.Aperture(1),
            dialAction(ExposureMode.MANUAL, CameraKeys.DIAL_2_CLOCKWISE),
        )
        assertEquals(
            DialAction.Aperture(-1),
            dialAction(ExposureMode.MANUAL, CameraKeys.DIAL_2_COUNTERCLOCKWISE),
        )
        assertEquals(DialAction.Iso(1), dialAction(ExposureMode.MANUAL, CameraKeys.WHEEL_CLOCKWISE))
        assertEquals(
            DialAction.Iso(-1),
            dialAction(ExposureMode.MANUAL, CameraKeys.WHEEL_COUNTERCLOCKWISE),
        )
    }

    @Test
    fun otherModesOnlyWheelChangesIso() {
        for (mode in listOf(ExposureMode.AUTO, ExposureMode.SCENE, ExposureMode.UNKNOWN)) {
            assertNull(dialAction(mode, CameraKeys.DIAL_1_CLOCKWISE))
            assertNull(dialAction(mode, CameraKeys.DIAL_2_CLOCKWISE))
            assertEquals(DialAction.Iso(1), dialAction(mode, CameraKeys.WHEEL_CLOCKWISE))
            assertEquals(DialAction.Iso(-1), dialAction(mode, CameraKeys.WHEEL_COUNTERCLOCKWISE))
        }
    }

    @Test
    fun controlWheelDial3ChangesIsoInEveryMode() {
        for (mode in ExposureMode.values()) {
            assertEquals(DialAction.Iso(1), dialAction(mode, CameraKeys.DIAL_3_CLOCKWISE))
            assertEquals(DialAction.Iso(-1), dialAction(mode, CameraKeys.DIAL_3_COUNTERCLOCKWISE))
        }
    }

    @Test
    fun unrelatedKeysReturnNull() {
        assertNull(dialAction(ExposureMode.PROGRAM, CameraKeys.ENTER))
        assertNull(dialAction(ExposureMode.MANUAL, CameraKeys.AEL))
        assertNull(dialAction(ExposureMode.SHUTTER, CameraKeys.MODE_DIAL))
    }
}
