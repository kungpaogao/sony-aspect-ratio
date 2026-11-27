package org.andrewgao.sony.aspectratio.camera

import android.app.Activity
import android.content.Intent

/**
 * Registers the app with the camera OS's arbiter, DAConnectionManagerService, as Sony's shooting
 * apps do on every resume through `AppInfo.notifyAppInfo` in their base app.
 *
 * The arbiter decides which keys pull the camera back to its own shooting mode. Without this it
 * keeps the last list it was given, often the app menu's, which includes the shutter half-press, so
 * a half-press could drop out of the app. [PULLING_BACK_KEYS] also lists ▶, KEY_PLAY, so playback
 * is the camera's own; leaving it lands in the camera's shooting mode and this app is reopened from
 * the menu.
 */
object CameraOs {
    private const val ACTION_APP_INFO =
        "com.android.server.DAConnectionManagerService.AppInfoReceive"

    /** Plugging in USB, and ▶, hand the camera back to its own modes; the app has no playback. */
    private val PULLING_BACK_KEYS = arrayOf("KEY_USB_CONNECT", "KEY_PLAY")

    /** Sony's `BaseApp.RESUME_KEYS_FOR_SHOOTING`: power-on and wake-up paths that return here. */
    private val RESUME_KEYS =
        arrayOf(
            "KEY_POWER_SLIDE_PON",
            "KEY_RELEASE_APO",
            "KEY_PLAY_APO",
            "KEY_MEDIA_INOUT_APO",
            "KEY_LENS_APO",
            "KEY_ACCESSORY_APO",
            "KEY_DEDICATED_APO",
            "KEY_POWER_APO",
            "KEY_PLAY_PON",
        )

    /** Call from onResume. Off the camera nothing receives the broadcast. */
    fun registerStillShootingApp(activity: Activity) {
        val intent = Intent(ACTION_APP_INFO)
        intent.putExtra("package_name", activity.packageName)
        intent.putExtra("class_name", activity.javaClass.name)
        intent.putExtra("large_category", "CATEGORY_REC")
        intent.putExtra("small_category", "STILL")
        intent.putExtra("pullingback_key", PULLING_BACK_KEYS)
        intent.putExtra("resume_key", RESUME_KEYS)
        activity.sendBroadcast(intent)
        Trace.line("registered with the camera OS as a still shooting app")
    }
}
