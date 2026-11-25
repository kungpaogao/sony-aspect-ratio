package org.andrewgao.sony.aspectratio.camera

import com.github.ma1co.openmemories.framework.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class Exposure(
    val sceneMode: String? = null,
    val shutterNumerator: Int = 0,
    val shutterDenominator: Int = 0,
    val apertureX100: Int = 0,
    val evIndex: Int = 0,
    val evStep: Float = 0f,
    val iso: Int = 0,
)

/** Live exposure read from the open camera's parameters and shutter/aperture listeners. */
class ShootingControls {
    private val mutableExposure = MutableStateFlow(Exposure())
    val exposure: StateFlow<Exposure> = mutableExposure

    private var camera: Camera? = null
    private val loggedFailures = mutableSetOf<String>()

    fun attach(active: Camera) {
        camera = active
        loggedFailures.clear()
        refresh()
        active.setShutterSpeedListener { n, d ->
            mutableExposure.value =
                mutableExposure.value.copy(shutterNumerator = n, shutterDenominator = d)
        }
        active.setApertureListener { ap ->
            mutableExposure.value = mutableExposure.value.copy(apertureX100 = ap)
        }
        val e = mutableExposure.value
        Trace.line(
            "exposure mode=${e.sceneMode} ${e.shutterNumerator}/${e.shutterDenominator} " +
                "f/${e.apertureX100} ev=${e.evIndex}x${e.evStep} iso=${e.iso}",
        )
    }

    fun detach() {
        camera?.setShutterSpeedListener(null)
        camera?.setApertureListener(null)
        camera = null
        loggedFailures.clear()
    }

    /** Re-reads every value from the camera parameters. */
    fun refresh() {
        val active = camera ?: return
        val params = read("parameters", null) { active.parameters } ?: return
        mutableExposure.value =
            Exposure(
                sceneMode = read("sceneMode", null) { params.sceneMode },
                shutterNumerator = read("shutterNumerator", 0) { params.shutterNumerator },
                shutterDenominator = read("shutterDenominator", 0) { params.shutterDenominator },
                apertureX100 = read("aperture", 0) { params.aperture },
                evIndex = read("evIndex", 0) { params.exposureCompensation },
                evStep = read("evStep", 0f) { params.exposureCompensationStep },
                iso = read("iso", 0) { params.iso },
            )
    }

    /**
     * Reads one value; a getter the emulator doesn't support falls back instead of failing the
     * rest.
     */
    private inline fun <T> read(name: String, fallback: T, getter: () -> T): T =
        try {
            getter()
        } catch (error: Exception) {
            logFailureOnce(name, error)
            fallback
        }

    private fun logFailureOnce(name: String, error: Exception) {
        if (loggedFailures.add(name)) {
            Trace.line("exposure $name " + error.message)
        }
    }
}
