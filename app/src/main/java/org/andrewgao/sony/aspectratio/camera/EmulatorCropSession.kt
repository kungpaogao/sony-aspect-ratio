package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.StillImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import org.andrewgao.sony.aspectratio.AspectRatio

/**
 * Off-camera the Android camera hands back the JPEG bytes from [Camera.setJpegListener]. They are
 * cropped and saved, and the live view resumes in place.
 */
class EmulatorCropSession(onStatus: (String) -> Unit) : CropSession(onStatus) {
    override suspend fun shoot(active: Camera, context: Context, aspect: AspectRatio): String {
        val jpeg = awaitJpeg(active)
        val image = StillImage.fromJpeg(jpeg)
        try {
            if (!image.isValid) return "jpeg decode failed (${jpeg.size} bytes)"
            return cropAndSave(
                context,
                image,
                aspect,
                "external",
            ) // JpegSaver ignores the media id off-camera.
        } finally {
            image.release()
        }
    }

    private suspend fun awaitJpeg(active: Camera): ByteArray =
        suspendCancellableCoroutine { continuation ->
            active.setJpegListener { jpeg ->
                active.setJpegListener(null)
                continuation.resume(jpeg)
            }
            continuation.invokeOnCancellation { active.setJpegListener(null) }
            try {
                active.takePicture()
            } catch (error: Exception) {
                active.setJpegListener(null)
                continuation.resumeWithException(error)
            }
        }
}
