package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraKeys

/** What the AF/MF/AEL button does for the current press. */
enum class FocusButton {
    AUTOFOCUS,
    MAGNIFY,
}

/**
 * Routes the AF/MF/AEL button like Sony's own KeyConverter. The a7R's button always sends 638
 * (AF_MF_AEL); the AF/MF ↔ AEL lever ([lever], from [CameraKeys.SLIDE_AEL_FOCUS]) tells which
 * function it has: 533 (AF/MF) magnifies, anything else focuses. Keys 532 and 533 sent directly
 * (the emulator) route themselves.
 */
fun focusButton(key: Int, lever: Int): FocusButton? =
    when (key) {
        CameraKeys.AF_MF_AEL ->
            if (lever == CameraKeys.AF_MF) FocusButton.MAGNIFY else FocusButton.AUTOFOCUS
        CameraKeys.AEL -> FocusButton.AUTOFOCUS
        CameraKeys.AF_MF -> FocusButton.MAGNIFY
        else -> null
    }
