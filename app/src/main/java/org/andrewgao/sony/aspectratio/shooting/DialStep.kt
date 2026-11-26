package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys

fun dialStep(key: Int): Int =
    when (key) {
        CameraKeys.DIAL_1_CLOCKWISE,
        CameraKeys.DIAL_2_CLOCKWISE,
        CameraKeys.WHEEL_CLOCKWISE,
        CameraKeys.DIAL_3_CLOCKWISE,
        CameraKeys.RING_CLOCKWISE -> 1
        CameraKeys.DIAL_1_COUNTERCLOCKWISE,
        CameraKeys.DIAL_2_COUNTERCLOCKWISE,
        CameraKeys.WHEEL_COUNTERCLOCKWISE,
        CameraKeys.DIAL_3_COUNTERCLOCKWISE,
        CameraKeys.RING_COUNTERCLOCKWISE -> -1
        else -> 0
    }
