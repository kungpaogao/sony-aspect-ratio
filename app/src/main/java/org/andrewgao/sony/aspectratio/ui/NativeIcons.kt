package org.andrewgao.sony.aspectratio.ui

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.github.ma1co.openmemories.framework.DeviceInfo
import org.andrewgao.sony.aspectratio.shooting.ExposureMode

/**
 * The camera's own OSD icons. They aren't bundled in Sony's apps; they are system resources
 * (package 0x01) that every Sony app references by these IDs. See docs/native-osd-icons.md. The IDs
 * point at unrelated drawables (or nothing) off the camera, so [drawable] returns null there and
 * the views show text instead.
 */
object NativeIcons {
    const val MODE_PROGRAM = 0x010803a1
    const val MODE_APERTURE = 0x010803a5
    const val MODE_SHUTTER = 0x010803a9
    const val MODE_MANUAL = 0x010803ad
    const val MODE_AUTO = 0x010804d5
    const val ISO_LABEL = 0x0108050b
    const val EV = 0x010810fc
    const val CARD = 0x01080d7f
    /** Battery icon for levels 0 (empty) to 4 (full). */
    private val BATTERY = intArrayOf(0x01080d4e, 0x01080509, 0x01080ca2, 0x01080335, 0x01080b0a)
    const val AF_DOT = 0x01080e84

    fun drawable(resources: Resources, id: Int): Drawable? {
        if (!DeviceInfo.getInstance().isCamera) return null
        return try {
            resources.getDrawable(id)
        } catch (_: Resources.NotFoundException) {
            null
        }
    }

    fun modeDrawable(resources: Resources, mode: ExposureMode): Drawable? {
        val id =
            when (mode) {
                ExposureMode.PROGRAM -> MODE_PROGRAM
                ExposureMode.APERTURE -> MODE_APERTURE
                ExposureMode.SHUTTER -> MODE_SHUTTER
                ExposureMode.MANUAL -> MODE_MANUAL
                ExposureMode.AUTO -> MODE_AUTO
                ExposureMode.SCENE,
                ExposureMode.UNKNOWN -> return null
            }
        return drawable(resources, id)
    }

    fun batteryDrawable(resources: Resources, level: Int): Drawable? =
        BATTERY.getOrNull(level)?.let { drawable(resources, it) }
}
