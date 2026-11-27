package org.andrewgao.sony.aspectratio.camera

import android.content.Context
import com.github.ma1co.openmemories.framework.Camera
import com.github.ma1co.openmemories.framework.ImageInfo
import com.github.ma1co.openmemories.framework.MediaManager
import com.github.ma1co.openmemories.framework.StillImage
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import org.andrewgao.sony.aspectratio.AspectRatio
import org.andrewgao.sony.aspectratio.shooting.rotationDegrees

/**
 * On a Sony camera the camera saves its own JPEG and [MediaManager.setImageAddedListener] reports
 * it. Decoding answers BUSY while the shooting camera is open, so this releases the camera, decodes
 * the stored picture with [StillImage.decodeImage], crops and saves it, then [recreateSurface]
 * brings the live view back.
 */
class DeviceCropSession(
    /**
     * Hides and reshows the preview SurfaceView. After a crop the camera is released, and reopening
     * it on the old surface fails with "Overlay Creation Failed"; the new surface's callback
     * reopens it.
     */
    private val recreateSurface: () -> Unit,
    onStatus: (String) -> Unit,
) : CropSession(onStatus) {
    private var media: MediaManager? = null

    override suspend fun shoot(active: Camera, context: Context, aspect: AspectRatio): String {
        val media = media ?: MediaManager.create(context).also { media = it }
        val id = awaitStoredImage(active, media)
        Trace.line("image added $id")
        // StillPlay's decodeImage answers BUSY, 0x1004, while the shooting camera is open; Sony's
        // ShootingState.onPause stops the preview and halts CameraEx before playback, so do the
        // same.
        release()
        return try {
            cropStored(context, media, id, aspect)
        } finally {
            recreateSurface()
        }
    }

    private suspend fun awaitStoredImage(active: Camera, media: MediaManager): Long =
        suspendCancellableCoroutine { continuation ->
            media.setImageAddedListener { id ->
                media.setImageAddedListener(null)
                continuation.resume(id)
            }
            continuation.invokeOnCancellation { media.setImageAddedListener(null) }
            try {
                active.takePicture()
            } catch (error: Exception) {
                media.setImageAddedListener(null)
                continuation.resumeWithException(error)
            }
        }

    private suspend fun cropStored(
        context: Context,
        media: MediaManager,
        id: Long,
        aspect: AspectRatio,
    ): String {
        val info = media.getImageInfo(id)
        try {
            val orientation = info.orientation
            val degrees = rotationDegrees(orientation)
            Trace.line("image $id orientation $orientation")
            val source = decode(info) ?: return "decodeImage failed for image $id"
            try {
                val status =
                    cropAndSave(
                        context,
                        source,
                        aspect,
                        media.mediaIds.firstOrNull() ?: "external",
                    )
                // JpegExporter saves with no EXIF orientation, so fix the crop like Photo Retouch
                // does.
                if (degrees != 0 && status.startsWith("saved ")) {
                    rotateSavedCrop(media, id, degrees, orientation)
                }
                return status
            } finally {
                source.release()
            }
        } finally {
            info.release()
        }
    }

    private fun rotateSavedCrop(
        media: MediaManager,
        originalId: Long,
        degrees: Int,
        orientation: Int,
    ) {
        val cursor = media.queryImages() ?: return
        try {
            val newId = if (cursor.moveToLast()) media.getImageId(cursor) else 0L
            if (newId <= originalId) {
                Trace.line("rotate crop: newest image $newId is not newer than $originalId")
                return
            }
            val ok = media.rotateImage(newId, degrees)
            Trace.line("rotated crop $newId by $degrees for orientation $orientation: $ok")
        } catch (error: Exception) {
            Trace.line("rotate crop " + describe(error))
        } finally {
            cursor.close()
        }
    }

    /** The decoder can stay busy for a moment after the camera closes, so retry. */
    private suspend fun decode(info: ImageInfo): StillImage? {
        repeat(DECODE_ATTEMPTS) { attempt ->
            if (attempt > 0) delay(DECODE_RETRY_MS)
            val image =
                try {
                    StillImage.decodeImage(info, StillImage.TYPE_FULL)
                } catch (error: Exception) {
                    Trace.line("decodeImage " + describe(error))
                    null
                }
            if (image != null && image.isValid) return image
            image?.release()
        }
        return null
    }

    companion object {
        private const val DECODE_ATTEMPTS = 3
        private const val DECODE_RETRY_MS = 300L
    }
}
