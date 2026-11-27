package org.andrewgao.sony.aspectratio.camera

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.Executors

/**
 * Appends a line to ar.txt on the card and to logcat. Logcat is written at once; the card is slow
 * (4-40 ms a line), so one background thread writes the file in order.
 */
object Trace {
    val file = File(Environment.getExternalStorageDirectory(), "ar.txt")

    private val writer = Executors.newSingleThreadExecutor { task ->
        Thread(task, "ar-trace").apply { isDaemon = true }
    }

    fun line(message: String) {
        Log.i(TAG, message)
        val text = System.currentTimeMillis().toString() + " " + message + "\n"
        writer.execute {
            try {
                FileOutputStream(file, true).use { it.write(text.toByteArray()) }
            } catch (error: IOException) {
                Log.w(TAG, "ar.txt " + error.message)
            }
        }
    }

    private const val TAG = "AspectRatioTrace"
}
