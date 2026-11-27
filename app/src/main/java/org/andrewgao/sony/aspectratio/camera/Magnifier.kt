package org.andrewgao.sony.aspectratio.camera

import android.os.Handler
import android.os.Looper
import com.github.ma1co.openmemories.framework.Camera
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.andrewgao.sony.aspectratio.shooting.FocusMode
import org.andrewgao.sony.aspectratio.shooting.Magnification
import org.andrewgao.sony.aspectratio.shooting.MagnifierGrid
import org.andrewgao.sony.aspectratio.shooting.nextLevel

/**
 * The camera's focus magnifier, or punch-in: cycled with the AF/MF button, moved with the d-pad,
 * and started by the camera's focus ring in MF and DMF when its "MF Assist" setting is on.
 *
 * Sony's callbacks may arrive off the main thread, so they are posted to the main looper before
 * they touch anything; every other call comes from the main thread too.
 */
class Magnifier(private val focusMode: () -> FocusMode) {
    private val mutableState = MutableStateFlow(Magnification())
    val state: StateFlow<Magnification> = mutableState

    private val handler = Handler(Looper.getMainLooper())
    private val stopAfterRing = Runnable { stop() }
    private val loggedFailures = mutableSetOf<String>()
    private var camera: Camera? = null
    private var levels = emptyList<Int>()
    private var ratios = emptyMap<Int, Int>()
    private var grid = MagnifierGrid(0)
    private var mfAssist = false
    private var mfAssistSeconds = 0

    /** True when the button started the magnification, which the MF Assist timeout never ends. */
    private var byButton = false
    private var lastX = 0
    private var lastY = 0

    fun attach(active: Camera) {
        camera = active
        loggedFailures.clear()
        val params = read("parameters", null) { active.parameters }
        // Sony lists 0 as "arbitrary"; there is no ratio to show for it.
        ratios =
            read("magnificationLevels", emptyList<Int>()) {
                    params?.supportedPreviewMagnification ?: emptyList()
                }
                .associateWith { level ->
                    read("magnificationRatio", 0) { active.getPreviewMagnificationRatio(level) }
                }
                .filterValues { it > 0 }
        levels = ratios.keys.toList()
        mfAssist = read("mfAssist", false) { params?.mfAssist ?: false }
        mfAssistSeconds = read("mfAssistSeconds", 0) { params?.mfAssistSeconds ?: 0 }
        grid = MagnifierGrid(ratios.values.minOrNull() ?: 0)
        byButton = false
        lastX = 0
        lastY = 0
        mutableState.value = Magnification()
        read("magnificationListener", Unit) {
            active.setMagnificationListener(
                object : Camera.MagnificationListener {
                    override fun onMagnificationChanged(
                        on: Boolean,
                        ratio: Int,
                        level: Int,
                        x: Int,
                        y: Int,
                    ) {
                        handler.post { changed(on, ratio, level, x, y) }
                    }

                    override fun onFocusRing(rotating: Boolean, x: Int, y: Int) {
                        handler.post { focusRing(rotating, x, y) }
                    }
                },
            )
        }
        Trace.line(
            "magnifier levels=" +
                levels.joinToString(", ", "[", "]") { "$it=${ratios[it]}" } +
                " mfAssist=$mfAssist seconds=$mfAssistSeconds",
        )
    }

    fun detach() {
        handler.removeCallbacks(stopAfterRing)
        val active = camera
        if (active != null) {
            if (mutableState.value.on) {
                try {
                    active.stopPreviewMagnification()
                } catch (error: Exception) {
                    Trace.line("magnifier stop " + error.message)
                }
            }
            try {
                active.setMagnificationListener(null)
            } catch (error: Exception) {
                Trace.line("magnifier listener " + error.message)
            }
        }
        camera = null
        byButton = false
        mutableState.value = Magnification()
        loggedFailures.clear()
    }

    fun cycle() {
        val current = mutableState.value
        val next = nextLevel(levels, if (current.on) current.level else null)
        if (next == null) {
            stop()
            return
        }
        val x = if (current.on) current.x else lastX
        val y = if (current.on) current.y else lastY
        if (magnify(next, x, y)) {
            byButton = true
            handler.removeCallbacks(stopAfterRing)
        }
    }

    fun move(dx: Int, dy: Int) {
        val current = mutableState.value
        if (!current.on) return
        val (x, y) = grid.moved(current.x, current.y, dx, dy)
        if (magnify(current.level, x, y)) {
            byButton = true
            handler.removeCallbacks(stopAfterRing)
        }
    }

    fun recenter() {
        val current = mutableState.value
        if (!current.on) return
        if (magnify(current.level, 0, 0)) {
            byButton = true
            handler.removeCallbacks(stopAfterRing)
        }
    }

    fun stop() {
        handler.removeCallbacks(stopAfterRing)
        val active = camera ?: return
        try {
            Trace.line("magnifier stop")
            active.stopPreviewMagnification()
        } catch (error: Exception) {
            Trace.line("magnifier stop " + error.message)
            return
        }
        byButton = false
        mutableState.value = Magnification()
    }

    /** True when the camera accepted the request. */
    private fun magnify(level: Int, x: Int, y: Int): Boolean {
        val active = camera ?: return false
        try {
            Trace.line("magnifier set level=$level at $x,$y")
            active.setPreviewMagnification(level, x, y)
        } catch (error: Exception) {
            Trace.line("magnifier set " + error.message)
            return false
        }
        lastX = x
        lastY = y
        // Optimistic; the camera's callback replaces it with what it actually did.
        mutableState.value = Magnification(true, ratios[level] ?: 0, level, x, y)
        return true
    }

    /** The camera is authoritative, including when firmware MF Assist magnifies on its own. */
    private fun changed(on: Boolean, ratio: Int, level: Int, x: Int, y: Int) {
        if (camera == null) return
        Trace.line("magnifier on=$on ratio=$ratio level=$level at $x,$y")
        if (!on) {
            handler.removeCallbacks(stopAfterRing)
            byButton = false
        }
        mutableState.value = Magnification(on, ratio, level, x, y)
    }

    private fun focusRing(rotating: Boolean, x: Int, y: Int) {
        if (camera == null) return
        val mode = focusMode()
        Trace.line("focus ring rotating=$rotating at $x,$y mode=$mode")
        handler.removeCallbacks(stopAfterRing)
        val current = mutableState.value
        if (rotating) {
            val manual = mode == FocusMode.MF || mode == FocusMode.DMF
            if (!current.on && mfAssist && manual && levels.isNotEmpty()) {
                // In DMF the point is where AF focused. In MF the a7R reports -1000,-1000, meaning
                // no point; like Sony's MF Assist, reuse the last position, which starts centered.
                val known = x != NO_POSITION || y != NO_POSITION
                val atX = if (known) x else lastX
                val atY = if (known) y else lastY
                if (magnify(levels[0], atX, atY)) byButton = false
            }
        } else if (current.on && !byButton && mfAssistSeconds > 0) {
            handler.postDelayed(stopAfterRing, mfAssistSeconds * 1000L)
        }
    }

    /** A getter the emulator doesn't support falls back instead of failing the rest. */
    private inline fun <T> read(name: String, fallback: T, getter: () -> T): T =
        try {
            getter()
        } catch (error: Exception) {
            if (loggedFailures.add(name)) {
                Trace.line("magnifier $name " + error.message)
            }
            fallback
        }

    private companion object {
        /** What the a7R reports for the focus-ring position when there is none, as in MF. */
        const val NO_POSITION = -1000
    }
}
