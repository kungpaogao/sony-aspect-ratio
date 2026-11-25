package org.andrewgao.sony.aspectratio.shooting

import org.andrewgao.sony.aspectratio.AspectRatio

data class ShootingState(
    val aspect: AspectRatio = AspectRatio.DEFAULT,
    val choosingRatio: Boolean = false,
    val shooting: Boolean = false,
    val message: String = "",
    val mode: String = "--",
    val shutter: String = "--",
    val aperture: String = "--",
    val exposure: String = "--",
    val iso: String = "--",
    val shots: String = "--",
    val battery: String = "--",
    val focus: Focus = Focus.IDLE,
    val exposureMode: ExposureMode = ExposureMode.UNKNOWN,
    val batteryLevel: Int = -1,
)
