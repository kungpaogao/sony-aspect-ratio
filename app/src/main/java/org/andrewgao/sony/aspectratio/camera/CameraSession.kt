package org.andrewgao.sony.aspectratio.camera

import android.hardware.Camera as AndroidCamera
import android.util.Log
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.Camera
import kotlin.math.roundToInt

/**
 * Opens the still preview, reads the attached lens, and releases both on the way out.
 * The framework uses CameraEx on a body and android.hardware.Camera on the emulator.
 */
class CameraSession {
    private var camera: Camera? = null
    private var previewing = false

    fun open(holder: SurfaceHolder): LensReading {
        val active = camera ?: openCamera() ?: return LensReading.unavailable()
        if (!previewing) {
            try {
                choosePreviewSize(active)
                active.setPreviewDisplay(holder)
                active.startPreview()
                previewing = true
            } catch (error: Exception) {
                Log.e(TAG, "preview failed", error)
                release()
                return LensReading.unavailable()
            }
        }
        return readLens(active)
    }

    fun release() {
        val active = camera ?: return
        camera = null
        previewing = false
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
            val focal = active.normalCamera?.parameters?.focalLength ?: return null
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
