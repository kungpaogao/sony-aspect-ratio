package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import android.graphics.Rect
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.github.ma1co.openmemories.framework.ImageFilters
import com.github.ma1co.openmemories.framework.JpegSaver
import com.github.ma1co.openmemories.framework.JpegSaver.QUALITY_EXTRA_FINE
import com.github.ma1co.openmemories.framework.MediaManager
import com.github.ma1co.openmemories.framework.StillImage
import com.github.ma1co.openmemories.framework.StillImage.TYPE_FULL
import com.github.ma1co.openmemories.framework.StillImage.TYPE_SCREEN
import org.andrewgao.sony.aspectratio.AspectRatio
import java.util.concurrent.atomic.AtomicBoolean
import android.hardware.Camera as AndroidCamera


/**
 * Opens the still preview, reads the attached lens, and releases both on the way out.
 * The framework uses CameraEx on a body and android.hardware.Camera on the emulator.
 */
class CameraSession {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var camera: Camera? = null

    //    private var sequence: CaptureSequence? = null
    private var deviceCamera: Camera.DeviceCamera? = null

    fun open(holder: SurfaceHolder) {
        try {
            val active = camera ?: openCamera()
            checkNotNull(active) { "no active camera" }
            choosePreviewSize(active)
            active.setPreviewDisplay(holder)
            active.startPreview()
            // The framework keeps every preview frame. This screen only draws over the image.
            active.normalCamera?.setPreviewCallback(null)
        } catch (error: Exception) {
            Log.e(TAG, "preview failed", error)
            release()
            return
        }
    }

//    fun cropImageFilter(optimizedImage: OptimizedImage?, cropRect: Rect?): OptimizedImage? {
//        try {
//            val filter = CropImageFilter()
//            filter.setSrcRect(cropRect)
//            filter.setSource(optimizedImage, false)
//            val isExec = filter.execute()
//            var cropImg: OptimizedImage? = null
//            if (isExec) {
//                cropImg = filter.getOutput()
//            }
//            filter.clearSources()
//            filter.release()
//            return cropImg
//        } catch (error: Exception) {
//            Trace.line("crop image filter " + error.javaClass.simpleName + " " + error.message)
//            return null
//        }
//    }

    fun takePicture(context: Context, aspect: AspectRatio, onLog: (String) -> Unit) {
        val active = camera
        if (active == null) {
            Log.e(TAG, "no active camera")
            return
        }
        val done = AtomicBoolean(false)
//        val storeStarted = AtomicBoolean(false)

        fun finish(message: String) {
            if (!done.compareAndSet(false, true)) return
            mainHandler.post {
                if (camera == active) resumePreview(active)
                onLog(message)
            }
        }

        active.setStoredImageListener { status, directory, filename ->
            active.setStoredImageListener(null)
            Log.d(TAG, "stored status=$status, dir=$directory, file=$filename")
        }
        active.setJpegListener { jpeg ->
            Log.d(TAG, "jpeg captured, size: ${jpeg.size}")
            try {
                // reset resources
                active.setJpegListener(null)
//                active.cancelTakePicture()
//                active.startPreview()
                finish("jpeg saved")
            } catch (error: Exception) {
                Trace.line("jpeg listener " + error.javaClass.simpleName + " " + error.message)
                finish("errored out")
            }
        }
        try {
            active.takePicture()
        } catch (error: Exception) {
            Log.e(TAG, "takePicture failed", error)
            active.setJpegListener(null)
            active.setStoredImageListener(null)
            finish("takePicture " + error.javaClass.simpleName + " " + error.message)
            return
        }
    }

    fun processLatestImage(context: Context) {
        try {

            // this is the error here, basically we need to create a device image
//                val image = StillImage.fromJpeg(jpeg)
            val mediaManager = MediaManager.create(context)
            val cursor = mediaManager.queryNewestImages()
            Log.d(TAG, "cursor length: ${cursor.count}")
            // move to newest image
            cursor.moveToFirst()
            val imageInfo = mediaManager.getImageInfo(cursor)
            cursor.close()
//                Log.d(TAG, "first imageInfo: $imageId")
//                val path = "content://com.sony.scalar.providers.avindex/1000/images/media/$imageId"
            val root = Environment.getExternalStorageDirectory().path
            Log.d(TAG, "root: $root")
            val path = "$root/DCIM/${imageInfo.folder}/${imageInfo.filename}"
            Log.d(TAG, "image path: $path")
            val image = StillImage.decodeFile(path, TYPE_FULL)
            // error when decoding the image
            // 1004 -> SCALAR_ERROR_BUSY
            // 1200 -> SCALAR_ERROR_INVALID_PATH
            Log.d(TAG, "image is device image: ${image is StillImage.DeviceImage}")
            // always invalid for some reason???????????
            Log.d(TAG, "image: ${image.isValid}")
            val croppedImage = ImageFilters.crop(
                image,
                Rect(100, 100, 200, 200)
            )
            Log.d(TAG, "image: ${croppedImage.isValid}")
            // returns null -> crashes when trying to get height/width
//                Log.d(TAG, "jpeg cropped, size: ${croppedImage.height}x${croppedImage.width}")
            val mediaId = mediaManager?.mediaIds?.firstOrNull()
            Log.d(TAG, "mediaId: $mediaId")
            val saved = JpegSaver.save(
                context,
                croppedImage,
                mediaId,
                QUALITY_EXTRA_FINE,
            )
            Log.d(TAG, "jpeg saved: $saved")
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    fun release() {
        val active = camera ?: return
        camera = null
        try {
            active.setJpegListener(null)
            active.setStoredImageListener(null)
        } catch (error: Exception) {
            Log.w(TAG, "listener clear failed", error)
        }
        try {
            active.stopPreview()
        } catch (error: Exception) {
            Log.w(TAG, "stopPreview failed", error)
        }
        try {
            active.release()
        } catch (error: Exception) {
            Log.e(TAG, "release failed", error)
        }
    }

    private fun resumePreview(active: Camera) {
        try {
            active.cancelTakePicture() // Resets Sony CameraEx capture state
        } catch (error: Exception) {
            Log.w(TAG, "cancelTakePicture failed", error)
        }
        try {
            active.startPreview()
            active.normalCamera?.setPreviewCallback(null)
        } catch (error: Exception) {
            Log.e(TAG, "preview restart failed", error)
        }
    }

    private fun openCamera(): Camera? {
        return try {
            val options = Camera.OpenOptions()
                .setPreview(true)
                .setInheritSetting(true)
                .setRecordingMode(Camera.RECORDING_STILL)
            val opened = Camera.open(options)
            if (DeviceInfo.getInstance().isCamera) {
                deviceCamera = opened as Camera.DeviceCamera
            }
            camera = opened
            opened
        } catch (error: Exception) {
            Log.e(TAG, "open failed", error)
            null
        }
    }

    private fun choosePreviewSize(active: Camera) {
        val androidCamera = active.normalCamera ?: return
        try {
            val parameters = androidCamera.parameters ?: return
            val sizes = parameters.supportedPreviewSizes ?: return
            val best = largest(sizes) ?: return
            parameters.setPreviewSize(best.width, best.height)
            androidCamera.parameters = parameters
        } catch (error: Exception) {
            Log.w(TAG, "preview size unchanged", error)
        }
    }

    private fun largest(sizes: List<AndroidCamera.Size>): AndroidCamera.Size? {
        var best: AndroidCamera.Size? = null
        for (size in sizes) {
            val current = best
            if (current == null || size.width * size.height > current.width * current.height) {
                best = size
            }
        }
        return best
    }

    companion object {
        private const val TAG = "CameraSession"
    }
}
