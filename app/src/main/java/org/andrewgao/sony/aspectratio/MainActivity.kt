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
import com.github.ma1co.openmemories.framework.DeviceInfo
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.andrewgao.sony.aspectratio.camera.CropSession
import org.andrewgao.sony.aspectratio.camera.Trace
import org.andrewgao.sony.aspectratio.ui.CropOverlayView

class MainActivity : Activity() {
    private val scope = MainScope()
    private var shot: Job? = null
    private val session =
        CropSession.create(recreateSurface = { recreateSurface() }, onStatus = { log(it) })
    private lateinit var surface: SurfaceView
    private lateinit var overlay: CropOverlayView
    private lateinit var logView: TextView
    private var holder: SurfaceHolder? = null
    private var resumed = false
    private var leaving = false
    private var choosingRatio = false
    private var aspect = AspectRatio.DEFAULT

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surface = findViewById(R.id.preview_surface) as SurfaceView
        overlay = findViewById(R.id.crop_overlay) as CropOverlayView
        logView = findViewById(R.id.log) as TextView
        // Required on API 10, which is what the camera emulator runs.
        @Suppress("DEPRECATION") surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    holder = surfaceHolder
                    startPreview()
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    holder = surfaceHolder
                    startPreview()
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                    if (holder == surfaceHolder) holder = null
                }
            },
        )
        if (!DeviceInfo.getInstance().isCamera) showDebugKeys()
        log("build ${BuildConfig.BUILD_TIME}")
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        startPreview()
    }

    override fun onPause() {
        resumed = false
        shot?.cancel()
        session.release()
        super.onPause()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount != 0) return super.onKeyDown(keyCode, event)
        val mapped = emulatorKey(keyCode) ?: CameraKeys.fromKeyEvent(event)
        if (
            mapped == CameraKeys.MENU ||
                mapped == CameraKeys.DELETE ||
                keyCode == KeyEvent.KEYCODE_BACK
        ) {
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
        (findViewById(R.id.debug_ok) as Button).setOnClickListener {
            handleControl(CameraKeys.ENTER)
        }
        (findViewById(R.id.debug_minus) as Button).setOnClickListener {
            handleControl(CameraKeys.DIAL_1_COUNTERCLOCKWISE)
        }
        (findViewById(R.id.debug_plus) as Button).setOnClickListener {
            handleControl(CameraKeys.DIAL_1_CLOCKWISE)
        }
        (findViewById(R.id.debug_shutter) as Button).setOnClickListener { shoot() }
    }

    private fun emulatorKey(keyCode: Int): Int? {
        if (DeviceInfo.getInstance().isCamera) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> CameraKeys.ENTER
            KeyEvent.KEYCODE_S -> CameraKeys.DIAL_1_COUNTERCLOCKWISE
            KeyEvent.KEYCODE_D -> CameraKeys.DIAL_1_CLOCKWISE
            else -> null
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

    private fun dialStep(key: Int): Int =
        when (key) {
            CameraKeys.DIAL_1_CLOCKWISE,
            CameraKeys.DIAL_2_CLOCKWISE,
            CameraKeys.RING_CLOCKWISE -> 1
            CameraKeys.DIAL_1_COUNTERCLOCKWISE,
            CameraKeys.DIAL_2_COUNTERCLOCKWISE,
            CameraKeys.RING_COUNTERCLOCKWISE -> -1
            else -> 0
        }

    private fun showAspect() {
        overlay.setAspect(aspect, choosingRatio)
    }

    private fun leave() {
        if (leaving) return
        leaving = true
        finish()
        startActivity(
            Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    private fun startPreview() {
        val surfaceHolder = holder ?: return
        if (!resumed) return
        session.open(surfaceHolder)
    }

    /**
     * GONE then VISIBLE destroys and recreates the surface; the new surface's callbacks reopen the
     * camera.
     */
    private fun recreateSurface() {
        surface.visibility = View.GONE
        surface.postDelayed({ surface.visibility = View.VISIBLE }, 100)
    }

    private fun shoot() {
        if (shot?.isActive == true || leaving) return
        log("shutter " + aspect.label)
        shot = scope.launch { log(session.takePicture(applicationContext, aspect)) }
    }

    private fun log(message: String) {
        Trace.line(message)
        logView.text = message
    }
}
