package org.andrewgao.sony.aspectratio.camera

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/** Appends a line to ar.txt on the card and to logcat. */
object Trace {
    val file = File(Environment.getExternalStorageDirectory(), "ar.txt")

    fun line(message: String) {
        Log.i(TAG, message)
        try {
            FileOutputStream(file, true).use {
                it.write(
                    (System.currentTimeMillis().toString() + " " + message + "\n").toByteArray(),
                )
            }
        } catch (error: IOException) {
            Log.w(TAG, "ar.txt " + error.message)
        }
    }

    private const val TAG = "AspectRatioTrace"
}
