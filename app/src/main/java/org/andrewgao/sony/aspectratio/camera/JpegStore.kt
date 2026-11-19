package org.andrewgao.sony.aspectratio.camera

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.github.ma1co.openmemories.framework.MediaManager
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.centeredCrop
import java.io.File
import java.io.FileOutputStream

object JpegStore {
    fun save(context: Context, jpeg: ByteArray, aspect: AspectRatio): String? {
        var decoder: BitmapRegionDecoder? = null
        try {
            decoder = BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size, false)
            val crop = centeredCrop(decoder.width, decoder.height, aspect)
            if (crop.width < 2 || crop.height < 2) return null
            val rect = Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height)
            val bitmap = decode(decoder, rect) ?: return null
            try {
                return write(context, bitmap)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        } catch (error: Exception) {
            Log.e(TAG, "save failed", error)
            return null
        } finally {
            try {
                decoder?.recycle()
            } catch (error: Exception) {
                Log.w(TAG, "decoder recycle failed", error)
            }
        }
    }

    private fun write(context: Context, bitmap: Bitmap): String? {
        val dir = cardDirectory()
        if (dir == null) {
            Log.e(TAG, "no writable DCIM directory")
            return null
        }
        val file = File(dir, "AR_" + System.currentTimeMillis() + ".jpg")
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(file)
            if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)) {
                Log.e(TAG, "compress failed")
                return null
            }
            out.flush()
        } catch (error: Exception) {
            Log.e(TAG, "write failed " + file.absolutePath, error)
            return null
        } finally {
            try {
                out?.close()
            } catch (error: Exception) {
                Log.w(TAG, "close failed", error)
            }
        }
        remember(context, file)
        indexOnCamera(context)
        Log.i(TAG, "wrote " + file.absolutePath)
        return file.absolutePath
    }

    private fun cardDirectory(): File? {
        val candidates = ArrayList<File>()
        val root = Environment.getExternalStorageDirectory()
        if (root != null) {
            candidates.add(File(root, "DCIM/100MSDCF"))
            candidates.add(File(root, "DCIM"))
        }
        try {
            val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
            if (dcim != null) {
                candidates.add(File(dcim, "100MSDCF"))
                candidates.add(dcim)
            }
        } catch (error: Exception) {
            Log.w(TAG, "public DCIM unavailable", error)
        }
        candidates.add(File("/mnt/sdcard/DCIM/100MSDCF"))
        candidates.add(File("/mnt/sdcard/DCIM"))
        candidates.add(File("/mnt/extsd/DCIM/100MSDCF"))
        candidates.add(File("/mnt/extsd/DCIM"))
        for (dir in candidates) {
            if (dir.isDirectory && dir.canWrite()) return dir
            if (dir.mkdirs() && dir.canWrite()) return dir
            Log.w(TAG, "not writable " + dir.absolutePath)
        }
        return null
    }

    private fun remember(context: Context, file: File) {
        try {
            val values = ContentValues()
            values.put(MediaStore.Images.Media.DATA, file.absolutePath)
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            values.put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            context.applicationContext.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )
        } catch (error: Exception) {
            Log.w(TAG, "media store insert failed", error)
        }
    }

    private fun decode(decoder: BitmapRegionDecoder, rect: Rect): Bitmap? {
        var sample = 1
        while (sample <= 8) {
            try {
                val options = BitmapFactory.Options()
                options.inSampleSize = sample
                options.inPreferredConfig = Bitmap.Config.RGB_565
                return decoder.decodeRegion(rect, options)
            } catch (oom: OutOfMemoryError) {
                Log.w(TAG, "retrying crop at sample $sample")
                sample *= 2
            }
        }
        return null
    }

    private fun indexOnCamera(context: Context) {
        if (!DeviceInfo.getInstance().isCamera) return
        try {
            val media = MediaManager.create(context)
            val ids = media.mediaIds
            if (ids != null && ids.isNotEmpty()) media.waitForDatabase(ids[0])
        } catch (error: Exception) {
            Log.w(TAG, "card index not updated", error)
        }
    }

    private const val TAG = "JpegStore"
}
