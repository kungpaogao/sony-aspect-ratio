package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.BitmapRegionDecoder
import android.graphics.Rect
import android.util.Log
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.github.ma1co.openmemories.framework.JpegSaver
import com.github.ma1co.openmemories.framework.MediaManager
import com.github.ma1co.openmemories.framework.StillImage
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.centeredCrop

object JpegStore {
    fun save(context: Context, jpeg: ByteArray, aspect: AspectRatio): String? {
        var decoder: BitmapRegionDecoder? = null
        try {
            decoder = BitmapRegionDecoder.newInstance(jpeg, 0, jpeg.size, false)
            val crop = centeredCrop(decoder.width, decoder.height, aspect)
            if (crop.width < 2 || crop.height < 2) return null
            val rect = Rect(crop.left, crop.top, crop.left + crop.width, crop.top + crop.height)
            val bitmap = decode(decoder, rect) ?: return null
            val image = StillImage.fromBitmap(bitmap)
            try {
                val path = JpegSaver.save(context.applicationContext, image, null, JpegSaver.QUALITY_FINE)
                if (path != null) indexOnCamera(context)
                return path
            } finally {
                image.release()
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
