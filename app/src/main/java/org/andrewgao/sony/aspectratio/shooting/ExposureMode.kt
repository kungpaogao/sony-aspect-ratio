package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraParameters

/** On a Sony camera the scene mode is the exposure mode. */
enum class ExposureMode(val label: String) {
    PROGRAM("P"),
    APERTURE("A"),
    SHUTTER("S"),
    MANUAL("M"),
    AUTO("iA"),
    SCENE("SCN"),
    UNKNOWN("--");

    companion object {
        fun fromSceneMode(sceneMode: String?): ExposureMode =
            when (sceneMode) {
                CameraParameters.SCENE_MODE_PROGRAM -> PROGRAM
                CameraParameters.SCENE_MODE_APERTURE -> APERTURE
                CameraParameters.SCENE_MODE_SHUTTER -> SHUTTER
                CameraParameters.SCENE_MODE_MANUAL -> MANUAL
                CameraParameters.SCENE_MODE_AUTO -> AUTO
                null -> UNKNOWN
                else -> SCENE
            }
    }
}
