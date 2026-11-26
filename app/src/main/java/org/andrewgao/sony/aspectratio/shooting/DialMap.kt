package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys

sealed class DialAction {
    data class ProgramShift(val steps: Int) : DialAction()

    data class Aperture(val steps: Int) : DialAction()

    data class Shutter(val steps: Int) : DialAction()

    data class Iso(val steps: Int) : DialAction()
}

/**
 * Maps a dial or wheel key (the a7R control wheel is DIAL_3) to what it should change in [mode].
 * Clockwise is +1.
 */
fun dialAction(mode: ExposureMode, key: Int): DialAction? {
    val steps =
        when (key) {
            CameraKeys.DIAL_1_CLOCKWISE,
            CameraKeys.DIAL_2_CLOCKWISE,
            CameraKeys.WHEEL_CLOCKWISE,
            CameraKeys.DIAL_3_CLOCKWISE -> 1
            CameraKeys.DIAL_1_COUNTERCLOCKWISE,
            CameraKeys.DIAL_2_COUNTERCLOCKWISE,
            CameraKeys.WHEEL_COUNTERCLOCKWISE,
            CameraKeys.DIAL_3_COUNTERCLOCKWISE -> -1
            else -> return null
        }
    if (
        key == CameraKeys.WHEEL_CLOCKWISE ||
            key == CameraKeys.WHEEL_COUNTERCLOCKWISE ||
            key == CameraKeys.DIAL_3_CLOCKWISE ||
            key == CameraKeys.DIAL_3_COUNTERCLOCKWISE
    ) {
        return DialAction.Iso(steps)
    }
    return when (mode) {
        ExposureMode.PROGRAM -> DialAction.ProgramShift(steps)
        ExposureMode.APERTURE -> DialAction.Aperture(steps)
        ExposureMode.SHUTTER -> DialAction.Shutter(steps)
        ExposureMode.MANUAL ->
            when (key) {
                CameraKeys.DIAL_1_CLOCKWISE,
                CameraKeys.DIAL_1_COUNTERCLOCKWISE -> DialAction.Shutter(steps)
                CameraKeys.DIAL_2_CLOCKWISE,
                CameraKeys.DIAL_2_COUNTERCLOCKWISE -> DialAction.Aperture(steps)
                else -> null
            }
        else -> null
    }
}
