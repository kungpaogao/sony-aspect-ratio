package org.andrewgao.sony.aspectratio.ui

import android.content.res.Resources
import android.graphics.drawable.Drawable
import com.github.ma1co.openmemories.framework.DeviceInfo
import org.andrewgao.sony.aspectratio.shooting.ExposureMode
import org.andrewgao.sony.aspectratio.shooting.FocusMode

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

    // From Sony's res/layout-notlong/icon_focusmode.xml: the 4:3 screen's set, in a 70x60 slot,
    // identical in digital-filter, star-trails and time-lapse. The layout-long and EVF layouts use
    // other IDs.
    const val FOCUS_AF_A = 0x01081369
    const val FOCUS_AF_S = 0x01081345
    const val FOCUS_AF_C = 0x0108138c
    const val FOCUS_MF = 0x010808a9
    const val FOCUS_DMF = 0x010808ab

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

    fun focusModeDrawable(resources: Resources, mode: FocusMode): Drawable? {
        val id =
            when (mode) {
                FocusMode.AF_S -> FOCUS_AF_S
                FocusMode.AF_C -> FOCUS_AF_C
                FocusMode.AF_A -> FOCUS_AF_A
                FocusMode.DMF -> FOCUS_DMF
                FocusMode.MF -> FOCUS_MF
                FocusMode.UNKNOWN -> return null
            }
        return drawable(resources, id)
    }

    fun batteryDrawable(resources: Resources, level: Int): Drawable? =
        BATTERY.getOrNull(level)?.let { drawable(resources, it) }
}
