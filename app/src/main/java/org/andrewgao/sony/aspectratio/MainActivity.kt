package org.andrewgao.sony.aspectratio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.github.ma1co.openmemories.framework.CameraKeys
import com.github.ma1co.openmemories.framework.CaptureSequence
import com.github.ma1co.openmemories.framework.DeviceInfo
import com.sony.scalar.hardware.CameraSequence
import org.andrewgao.sony.aspectratio.camera.CameraSession
import org.andrewgao.sony.aspectratio.camera.Trace
import org.andrewgao.sony.aspectratio.ui.CropOverlayView

class MainActivity : Activity() {
    private val session = CameraSession()
    private var holder: SurfaceHolder? = null
    private var surfaceReady = false
    private var resumed = false
    private var leaving = false
    private var choosingRatio = false
    private var aspect = AspectRatio.DEFAULT
    private var saving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val surface = findViewById(R.id.preview_surface) as SurfaceView
        // Required on API 10, which is what the camera emulator runs.
        @Suppress("DEPRECATION")
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                holder = surfaceHolder
                surfaceReady = true
                startPreview()
            }

            override fun surfaceChanged(
                surfaceHolder: SurfaceHolder,
                format: Int,
                width: Int,
                height: Int
            ) {
                holder = surfaceHolder
                surfaceReady = true
                startPreview()
            }

            override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                surfaceReady = false
                if (holder == surfaceHolder) holder = null
            }
        })
        if (!onCamera()) showDebugKeys()
        log("15 start " + Trace.location())
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        startPreview()
    }

    override fun onPause() {
        resumed = false
        session.release()
        super.onPause()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount != 0) return super.onKeyDown(keyCode, event)
        val mapped = emulatorKey(keyCode) ?: cameraKey(event)
        if (mapped == CameraKeys.MENU || mapped == CameraKeys.DELETE || keyCode == KeyEvent.KEYCODE_BACK) {
            leave()
            return true
        }
        if (handleControl(mapped)) return true
        if (mapped == CameraKeys.SHUTTER || keyCode == KeyEvent.KEYCODE_CAMERA) {
            shoot()
            return true
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun showDebugKeys() {
        val bar = findViewById(R.id.debug_keys) as View
        bar.visibility = View.VISIBLE
        (findViewById(R.id.debug_ok) as Button).setOnClickListener { handleControl(CameraKeys.ENTER) }
        (findViewById(R.id.debug_minus) as Button).setOnClickListener {
            handleControl(CameraKeys.DIAL_1_COUNTERCLOCKWISE)
        }
        (findViewById(R.id.debug_plus) as Button).setOnClickListener {
            handleControl(CameraKeys.DIAL_1_CLOCKWISE)
        }
        (findViewById(R.id.debug_shutter) as Button).setOnClickListener { shoot() }
    }

    private fun emulatorKey(keyCode: Int): Int? {
        if (onCamera()) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> CameraKeys.ENTER
            KeyEvent.KEYCODE_S -> CameraKeys.DIAL_1_COUNTERCLOCKWISE
            KeyEvent.KEYCODE_D -> CameraKeys.DIAL_1_CLOCKWISE
            else -> null
        }
    }

    private fun cameraKey(event: KeyEvent): Int {
        return try {
            CameraKeys.fromKeyEvent(event)
        } catch (_: Exception) {
            0
        }
    }

    private fun handleControl(mapped: Int): Boolean {
        if (mapped == CameraKeys.ENTER) {
            choosingRatio = !choosingRatio
            showAspect()
            return true
        }
        val dial = if (choosingRatio) dialStep(mapped) else 0
        if (dial != 0) {
            aspect = AspectRatio.step(aspect, dial)
            showAspect()
            return true
        }
        return false
    }

    private fun onCamera(): Boolean {
        return try {
            DeviceInfo.getInstance().isCamera
        } catch (_: Exception) {
            false
        }
    }

    private fun dialStep(key: Int): Int {
        if (key == CameraKeys.DIAL_1_CLOCKWISE || key == CameraKeys.DIAL_2_CLOCKWISE || key == CameraKeys.RING_CLOCKWISE) {
            return 1
        }
        if (key == CameraKeys.DIAL_1_COUNTERCLOCKWISE || key == CameraKeys.DIAL_2_COUNTERCLOCKWISE || key == CameraKeys.RING_COUNTERCLOCKWISE) {
            return -1
        }
        return 0
    }

    private fun showAspect() {
        val overlay = findViewById(R.id.crop_overlay) as CropOverlayView
        overlay.setAspect(aspect, choosingRatio)
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        session.release()
        finish()
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun startPreview() {
        val surfaceHolder = holder
        if (!resumed || !surfaceReady || surfaceHolder == null) return

        session.open(surfaceHolder)
    }

    private fun shoot() {
        if (saving || leaving) return
        saving = true
        log("shutter " + aspect.label)
        session.takePicture(applicationContext, aspect) { message ->
            if (leaving) return@takePicture
            val finished = !message.startsWith("jpeg ") && !message.startsWith("stored ")
            if (finished) saving = false
            session.processLatestImage(applicationContext)
            log(message)
        }
    }

    private fun log(message: String) {
        Trace.line(message)
        val text = findViewById(R.id.log) as TextView
        text.text = message
    }
}
