package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import android.view.SurfaceHolder
import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.github.ma1co.openmemories.framework.ImageFilters
import com.github.ma1co.openmemories.framework.JpegSaver
import com.github.ma1co.openmemories.framework.StillImage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.CropRect

/**
 * Owns the camera while the app is in front: shows the live view, takes a picture, crops it to the
 * selected aspect, and saves the crop as a second JPEG. The original stays on the card.
 *
 * How the picture comes back differs by device, so [create] picks a subclass, like the framework's
 * Camera / DeviceCamera:
 * - [DeviceCropSession] on a Sony camera reads the stored picture back from the card.
 * - [EmulatorCropSession] elsewhere gets the JPEG bytes from the Android camera.
 */
abstract class CropSession(private val onStatus: (String) -> Unit) {
    private var camera: Camera? = null
    val controls = ShootingControls()

    /** The camera's Auto Review setting, put back on release; null until it has been read. */
    private var savedAutoReview: Int? = null

    fun open(holder: SurfaceHolder) {
        try {
            val newlyOpened = camera == null
            val active =
                camera
                    ?: Camera.open(
                            Camera.OpenOptions()
                                .setPreview(true)
                                .setInheritSetting(true)
                                .setRecordingMode(Camera.RECORDING_STILL),
                        )
                        .also { camera = it }
            active.setPreviewDisplay(holder)
            startPreview(active)
            if (newlyOpened) {
                turnOffAutoReview(active)
                controls.attach(active)
            }
        } catch (error: Exception) {
            onStatus("camera " + describe(error))
            release()
        }
    }

    suspend fun takePicture(context: Context, aspect: AspectRatio): String {
        val active = camera ?: return "no active camera"
        val message =
            try {
                withTimeoutOrNull(TIMEOUT_MS) { shoot(active, context, aspect) }
                    ?: "timed out waiting for the camera"
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                describe(error)
            }
        // DeviceCropSession releases the camera to read the picture; otherwise the live view
        // resumes here.
        if (camera === active) resumePreview(active)
        return message
    }

    fun release() {
        val active = camera ?: return
        camera = null
        controls.detach()
        restoreAutoReview(active)
        try {
            active.stopPreview()
        } catch (error: Exception) {
            Trace.line("stopPreview " + describe(error))
        }
        try {
            // DeviceCamera.release frees the normal Camera before CameraEx, so the preview overlay
            // is not held.
            active.release()
        } catch (error: Exception) {
            Trace.line("release " + describe(error))
        }
    }

    /** Takes one picture with [active], then crops and saves it. Returns the status line. */
    protected abstract suspend fun shoot(
        active: Camera,
        context: Context,
        aspect: AspectRatio,
    ): String

    /** Crops [source] to [aspect] and saves the crop as a new JPEG. */
    protected fun cropAndSave(
        context: Context,
        source: StillImage,
        aspect: AspectRatio,
        mediaId: String,
    ): String {
        val width = source.width
        val height = source.height
        val crop = CropRect.aligned(width, height, aspect)
        Trace.line("crop ${width}x$height to $crop")
        if (crop.isWholeFrame(width, height))
            return "${aspect.label} is the native frame, nothing to crop"
        val cropped =
            ImageFilters.crop(source, crop.toRect()) ?: return "crop failed on ${width}x$height"
        try {
            if (!cropped.isValid) return "crop failed on ${width}x$height"
            JpegSaver.save(context, cropped, mediaId, JpegSaver.QUALITY_EXTRA_FINE)
                ?: return "save failed"
            return "saved ${aspect.label} ${crop.width}x${crop.height}"
        } finally {
            cropped.release()
        }
    }

    /**
     * The camera's review would show the whole frame while the crop is being made, so it is off
     * while the app has the camera, as in Sony's processing apps.
     */
    private fun turnOffAutoReview(active: Camera) {
        try {
            val time = active.autoReviewTime
            savedAutoReview = time
            if (time != 0) active.autoReviewTime = 0
            Trace.line("auto review $time, off while shooting")
        } catch (error: Exception) {
            Trace.line("auto review " + describe(error))
        }
    }

    private fun restoreAutoReview(active: Camera) {
        val time = savedAutoReview ?: return
        savedAutoReview = null
        if (time == 0) return
        try {
            active.autoReviewTime = time
        } catch (error: Exception) {
            Trace.line("auto review restore " + describe(error))
        }
    }

    private fun startPreview(active: Camera) {
        active.startPreview()
        // The framework copies every preview frame; this screen only draws over the live view.
        active.normalCamera?.setPreviewCallback(null)
    }

    private fun resumePreview(active: Camera) {
        try {
            active.cancelTakePicture() // Resets Sony CameraEx capture state
        } catch (error: Exception) {
            Trace.line("cancelTakePicture " + describe(error))
        }
        try {
            startPreview(active)
        } catch (error: Exception) {
            Trace.line("preview restart " + describe(error))
        }
    }

    protected fun describe(error: Throwable): String =
        error.javaClass.simpleName + " " + error.message

    companion object {
        fun create(recreateSurface: () -> Unit, onStatus: (String) -> Unit): CropSession =
            if (DeviceInfo.getInstance().isCamera) DeviceCropSession(recreateSurface, onStatus)
            else EmulatorCropSession(onStatus)

        /** Safety net so the shutter never stays locked. Covers long exposures plus NR. */
        private const val TIMEOUT_MS = 90_000L
    }
}
