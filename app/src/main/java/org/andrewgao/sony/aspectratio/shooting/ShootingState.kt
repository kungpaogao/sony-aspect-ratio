package org.andrewgao.sony.aspectratio.shooting

import org.andrewgao.sony.aspectratio.AspectRatio

data class ShootingState(
    val aspect: AspectRatio = AspectRatio.DEFAULT,
    val choosingRatio: Boolean = false,
    val shooting: Boolean = false,
    val message: String = "",
)
