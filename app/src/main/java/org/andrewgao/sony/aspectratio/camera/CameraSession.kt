package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import android.hardware.Camera as AndroidCamera
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.Camera
import org.andrewgao.sony.aspectratio.AspectRatio
import kotlin.math.roundToInt

/**
 * Opens the still preview, reads the attached lens, and releases both on the way out.
 * The framework uses CameraEx on a body and android.hardware.Camera on the emulator.
 */
class CameraSession {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var camera: Camera? = null
    private var previewing = false
    private var capturing = false

    fun open(holder: SurfaceHolder): LensReading {
        val active = camera ?: openCamera() ?: return LensReading.unavailable()
        if (!previewing && !capturing) {
            try {
                choosePreviewSize(active)
                active.setPreviewDisplay(holder)
                active.startPreview()
                // The framework keeps every preview frame. This screen only draws over the image.
                active.normalCamera?.setPreviewCallback(null)
                previewing = true
            } catch (error: Exception) {
                Log.e(TAG, "preview failed", error)
                release()
                return LensReading.unavailable()
            }
        }
        return readLens(active)
    }

    fun takePicture(context: Context, aspect: AspectRatio, onDone: (String?) -> Unit) {
        val active = camera
        if (active == null || capturing || !previewing) {
            Log.w(TAG, "shutter ignored camera=" + (active != null) + " capturing=" + capturing + " previewing=" + previewing)
            if (!capturing) onDone(null)
            return
        }
        capturing = true
        previewing = false
        active.setJpegListener { jpeg ->
            active.setJpegListener(null)
            Log.i(TAG, "jpeg bytes=" + (jpeg?.size ?: -1))
            Thread {
                val path = try {
                    if (jpeg == null || jpeg.isEmpty()) null else JpegStore.save(context, jpeg, aspect)
                } catch (error: Exception) {
                    Log.e(TAG, "save failed", error)
                    null
                }
                mainHandler.post {
                    capturing = false
                    if (camera == active) resumePreview(active)
                    onDone(path)
                }
            }.start()
        }
        try {
            active.takePicture()
        } catch (error: Exception) {
            Log.e(TAG, "takePicture failed", error)
            active.setJpegListener(null)
            capturing = false
            resumePreview(active)
            onDone(null)
        }
    }

    fun release() {
        val active = camera ?: return
        camera = null
        previewing = false
        capturing = false
        try {
            active.setJpegListener(null)
        } catch (error: Exception) {
            Log.w(TAG, "jpeg listener clear failed", error)
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
            active.startPreview()
            active.normalCamera?.setPreviewCallback(null)
            previewing = true
        } catch (error: Exception) {
            Log.e(TAG, "preview restart failed", error)
            previewing = false
        }
    }

    private fun openCamera(): Camera? {
        return try {
            val options = Camera.OpenOptions()
                .setPreview(true)
                .setInheritSetting(true)
                .setRecordingMode(Camera.RECORDING_STILL)
            val opened = Camera.open(options)
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

    private fun readLens(active: Camera): LensReading {
        val lens = try {
            active.lens
        } catch (error: Exception) {
            Log.w(TAG, "lens info failed", error)
            null
        }
        val wide = lens?.wideFocalLength ?: 0
        val tele = lens?.teleFocalLength ?: 0
        val focal = when {
            wide > 0 && tele > wide -> "$wide-$tele mm"
            wide > 0 -> "$wide mm"
            else -> androidFocalLength(active)
        }
        return LensReading(
            available = true,
            name = lens?.name,
            mount = lens?.type,
            focal = focal
        )
    }

    private fun androidFocalLength(active: Camera): String? {
        return try {
            val parameters = active.normalCamera?.parameters ?: return null
            val raw = parameters.get("focal-length")
            if (raw.isNullOrEmpty()) return null
            val focal = raw.toFloat()
            if (focal <= 0f) null else formatFocal(focal) + " mm"
        } catch (error: Exception) {
            Log.w(TAG, "focal length unavailable", error)
            null
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

    private fun formatFocal(value: Float): String {
        val tenths = (value * 10f).roundToInt()
        val whole = tenths / 10
        val fraction = tenths % 10
        return if (fraction == 0) whole.toString() else "$whole.$fraction"
    }

    companion object {
        private const val TAG = "CameraSession"
    }
}

class LensReading(
    val available: Boolean,
    val name: String?,
    val mount: String?,
    val focal: String?
) {
    companion object {
        fun unavailable(): LensReading = LensReading(false, null, null, null)
    }
}
