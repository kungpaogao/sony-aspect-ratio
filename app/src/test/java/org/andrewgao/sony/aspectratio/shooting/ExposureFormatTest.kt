package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraParameters
import org.junit.Assert.assertEquals
import org.junit.Test

class ExposureFormatTest {
    @Test
    fun modeFromSceneMode() {
        assertEquals(
            ExposureMode.PROGRAM,
            ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_PROGRAM),
        )
        assertEquals(
            ExposureMode.APERTURE,
            ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_APERTURE),
        )
        assertEquals(
            ExposureMode.SHUTTER,
            ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_SHUTTER),
        )
        assertEquals(
            ExposureMode.MANUAL,
            ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_MANUAL),
        )
        assertEquals(
            ExposureMode.AUTO,
            ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_AUTO),
        )
        assertEquals(ExposureMode.SCENE, ExposureMode.fromSceneMode("portrait"))
        assertEquals(ExposureMode.UNKNOWN, ExposureMode.fromSceneMode(null))
    }

    @Test
    fun modeLabels() {
        assertEquals("P", ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_PROGRAM).label)
        assertEquals("A", ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_APERTURE).label)
        assertEquals("S", ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_SHUTTER).label)
        assertEquals("M", ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_MANUAL).label)
        assertEquals("iA", ExposureMode.fromSceneMode(CameraParameters.SCENE_MODE_AUTO).label)
        assertEquals("SCN", ExposureMode.fromSceneMode("portrait").label)
        assertEquals("--", ExposureMode.fromSceneMode(null).label)
    }

    @Test
    fun shutterFormats() {
        assertEquals("BULB", ExposureFormat.shutter(65535, 1))
        assertEquals("1/3", ExposureFormat.shutter(1, 3))
        assertEquals("0.5\"", ExposureFormat.shutter(1, 2))
        assertEquals("30\"", ExposureFormat.shutter(30, 1))
        // 0.35 rounds up like Sony's BigDecimal(String) rounding, not down like the float 0.3499...
        assertEquals("0.4\"", ExposureFormat.shutter(7, 20))
        assertEquals("--", ExposureFormat.shutter(1, 0))
    }

    @Test
    fun apertureFormats() {
        assertEquals("F--", ExposureFormat.aperture(0))
        assertEquals("F2.8", ExposureFormat.aperture(280))
        assertEquals("F11", ExposureFormat.aperture(1100))
    }

    @Test
    fun exposureFormats() {
        assertEquals("±0.0", ExposureFormat.exposure(0, 0.333f))
        assertEquals("+0.3", ExposureFormat.exposure(1, 0.333f))
        assertEquals("-1.0", ExposureFormat.exposure(-3, 0.333f))
    }

    @Test
    fun isoFormats() {
        assertEquals("AUTO", ExposureFormat.iso(0))
        assertEquals("400", ExposureFormat.iso(400))
    }

    @Test
    fun shotsFormats() {
        assertEquals("", ExposureFormat.shots(-1))
        assertEquals("100", ExposureFormat.shots(100))
        assertEquals("9999", ExposureFormat.shots(10_000))
    }

    @Test
    fun batteryFormats() {
        assertEquals("", ExposureFormat.battery(-1))
        assertEquals("85%", ExposureFormat.battery(85))
    }

    @Test
    fun batteryLevelThresholds() {
        assertEquals(-1, ExposureFormat.batteryLevel(-1))
        assertEquals(4, ExposureFormat.batteryLevel(81))
        assertEquals(3, ExposureFormat.batteryLevel(51))
        assertEquals(2, ExposureFormat.batteryLevel(21))
        assertEquals(1, ExposureFormat.batteryLevel(1))
        assertEquals(0, ExposureFormat.batteryLevel(0))
        assertEquals(3, ExposureFormat.batteryLevel(80))
        assertEquals(2, ExposureFormat.batteryLevel(50))
        assertEquals(1, ExposureFormat.batteryLevel(20))
    }
}
