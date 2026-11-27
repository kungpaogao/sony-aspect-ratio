package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys

enum class FocusButton {
    AUTOFOCUS,
    MAGNIFY,
}

/**
 * Routes the AF/MF/AEL button like Sony's own KeyConverter. The a7R's button always sends 638,
 * AF_MF_AEL, and [lever], the AF/MF ↔ AEL lever read from [CameraKeys.SLIDE_AEL_FOCUS], tells which
 * function it has: 533, AF/MF, magnifies and anything else focuses. The emulator sends keys 532 and
 * 533 directly, and they route themselves.
 */
fun focusButton(key: Int, lever: Int): FocusButton? =
    when (key) {
        CameraKeys.AF_MF_AEL ->
            if (lever == CameraKeys.AF_MF) FocusButton.MAGNIFY else FocusButton.AUTOFOCUS
        CameraKeys.AEL -> FocusButton.AUTOFOCUS
        CameraKeys.AF_MF -> FocusButton.MAGNIFY
        else -> null
    }
