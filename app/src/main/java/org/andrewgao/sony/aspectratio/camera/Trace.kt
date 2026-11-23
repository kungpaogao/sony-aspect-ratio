package org.andrewgao.sony.aspectratio.camera

import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream

/** Appends a line to ar.txt on the card and to logcat. */
object Trace {
    fun line(message: String) {
        Log.i(TAG, message)
        val text = System.currentTimeMillis().toString() + " " + message + "\n"
        val seen = HashSet<String>()
        for (file in targets()) {
            val key = try {
                file.canonicalPath
            } catch (error: Exception) {
                file.absolutePath
            }
            if (!seen.add(key)) continue
            var out: FileOutputStream? = null
            try {
                val parent = file.parentFile
                if (parent != null && !parent.exists()) parent.mkdirs()
                out = FileOutputStream(file, true)
                out.write(text.toByteArray())
                out.flush()
            } catch (error: Exception) {
                Log.w(TAG, "ar.txt " + file.absolutePath + " " + error.message)
            } finally {
                try {
                    out?.close()
                } catch (error: Exception) {
                    Log.w(TAG, "ar.txt close " + error.message)
                }
            }
        }
    }

    fun location(): String {
        for (file in targets()) {
            if (file.isFile && file.canWrite()) return file.absolutePath
            val parent = file.parentFile
            if (parent != null && parent.isDirectory && parent.canWrite()) return file.absolutePath
        }
        return "ar.txt not writable"
    }

    private fun targets(): List<File> {
        val files = ArrayList<File>()
        val external = Environment.getExternalStorageDirectory()
        if (external != null) files.add(File(external, "ar.txt"))
        files.add(File("/sdcard/ar.txt"))
        files.add(File("/mnt/sdcard/ar.txt"))
        return files
    }

    private const val TAG = "AspectRatio"
}
