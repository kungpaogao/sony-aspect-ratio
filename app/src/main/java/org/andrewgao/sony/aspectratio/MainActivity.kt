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
import org.andrewgao.sony.aspectratio.shared.StateBinding
import org.andrewgao.sony.aspectratio.shooting.ShootingState
import org.andrewgao.sony.aspectratio.shooting.ShootingViewModel
import org.andrewgao.sony.aspectratio.ui.CropOverlayView

class MainActivity : Activity() {
    private lateinit var viewModel: ShootingViewModel
    private lateinit var binding: StateBinding<ShootingState>
    private lateinit var surface: SurfaceView
    private lateinit var overlay: CropOverlayView
    private lateinit var logView: TextView
    private var leaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surface = findViewById(R.id.preview_surface) as SurfaceView
        overlay = findViewById(R.id.crop_overlay) as CropOverlayView
        logView = findViewById(R.id.log) as TextView
        viewModel =
            ShootingViewModel(
                applicationContext,
                ::recreateSurface,
                "build ${BuildConfig.BUILD_TIME}",
            )
        binding = StateBinding(viewModel.state, ::render)
        // Required on API 10, which is what the camera emulator runs.
        @Suppress("DEPRECATION") surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        surface.holder.addCallback(
            object : SurfaceHolder.Callback {
                override fun surfaceCreated(surfaceHolder: SurfaceHolder) {
                    viewModel.onSurface(surfaceHolder)
                }

                override fun surfaceChanged(
                    surfaceHolder: SurfaceHolder,
                    format: Int,
                    width: Int,
                    height: Int,
                ) {
                    viewModel.onSurface(surfaceHolder)
                }

                override fun surfaceDestroyed(surfaceHolder: SurfaceHolder) {
                    viewModel.clearSurface(surfaceHolder)
                }
            },
        )
        if (!DeviceInfo.getInstance().isCamera) showDebugKeys()
    }

    override fun onResume() {
        super.onResume()
        viewModel.onStart()
        binding.start()
    }

    override fun onPause() {
        binding.stop()
        viewModel.onStop()
        super.onPause()
    }

    override fun onDestroy() {
        viewModel.clear()
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
        if (viewModel.onKeyDown(mapped)) return true
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
            viewModel.onKeyDown(CameraKeys.ENTER)
        }
        (findViewById(R.id.debug_minus) as Button).setOnClickListener {
            viewModel.onKeyDown(CameraKeys.DIAL_1_COUNTERCLOCKWISE)
        }
        (findViewById(R.id.debug_plus) as Button).setOnClickListener {
            viewModel.onKeyDown(CameraKeys.DIAL_1_CLOCKWISE)
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

    private fun render(state: ShootingState) {
        overlay.setAspect(state.aspect, state.choosingRatio)
        logView.text = state.message
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

    /**
     * GONE then VISIBLE destroys and recreates the surface; the new surface's callbacks reopen the
     * camera.
     */
    private fun recreateSurface() {
        surface.visibility = View.GONE
        surface.postDelayed({ surface.visibility = View.VISIBLE }, 100)
    }

    private fun shoot() {
        if (leaving) return
        viewModel.shoot()
    }
}
