package org.andrewgao.sony.aspectratio.shooting

/**
 * Maps EXIF orientation on the original still to degrees for
 * [com.github.ma1co.openmemories.framework.MediaManager.rotateImage].
 */
fun rotationDegrees(exifOrientation: Int): Int =
    when (exifOrientation) {
        6 -> 90
        3 -> 180
        8 -> -90
        else -> 0
    }
