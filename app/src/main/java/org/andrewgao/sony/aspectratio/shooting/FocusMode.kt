package org.andrewgao.sony.aspectratio.shooting

import com.github.ma1co.openmemories.framework.CameraParameters

/**
 * The camera's focus mode setting, not to be confused with [Focus], which is the state of a focus
 * attempt in progress.
 */
enum class FocusMode(val label: String) {
    AF_S("AF-S"),
    AF_C("AF-C"),
    AF_A("AF-A"),
    DMF("DMF"),
    MF("MF"),
    UNKNOWN("");

    companion object {
        /**
         * [focusMode] and [autoFocusMode] come from the camera's `getFocusMode()` and
         * `getAutoFocusMode()`. Sony shows its movie AF mode as AF-C.
         */
        fun from(focusMode: String?, autoFocusMode: String?): FocusMode =
            when (focusMode) {
                CameraParameters.FOCUS_MANUAL -> MF
                CameraParameters.FOCUS_DMF -> DMF
                CameraParameters.FOCUS_AUTO ->
                    when (autoFocusMode) {
                        "af-s" -> AF_S
                        "af-c",
                        "movie" -> AF_C
                        "af-a" -> AF_A
                        else -> UNKNOWN
                    }
                else -> UNKNOWN
            }
    }
}
