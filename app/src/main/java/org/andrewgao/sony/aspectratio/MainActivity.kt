package org.andrewgao.sony.aspectratio

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.KeyEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import com.github.ma1co.openmemories.framework.CameraKeys
import com.github.ma1co.openmemories.framework.DeviceInfo
import org.andrewgao.sony.aspectratio.camera.Trace
import org.andrewgao.sony.aspectratio.shared.StateBinding
import org.andrewgao.sony.aspectratio.shooting.Focus
import org.andrewgao.sony.aspectratio.shooting.FocusMode
import org.andrewgao.sony.aspectratio.shooting.ShootingState
import org.andrewgao.sony.aspectratio.shooting.ShootingViewModel
import org.andrewgao.sony.aspectratio.ui.CropOverlayView
import org.andrewgao.sony.aspectratio.ui.NativeIcons
import org.andrewgao.sony.aspectratio.ui.OsdTypeface
import org.andrewgao.sony.aspectratio.ui.reserveWidthFor

class MainActivity : Activity() {
    private lateinit var viewModel: ShootingViewModel
    private lateinit var binding: StateBinding<ShootingState>
    private lateinit var surface: SurfaceView
    private lateinit var overlay: CropOverlayView
    private lateinit var processingText: TextView
    private lateinit var modeIcon: ImageView
    private lateinit var modeText: TextView
    private lateinit var aspectChip: TextView
    private lateinit var shotsIcon: ImageView
    private lateinit var shotsText: TextView
    private lateinit var batteryIcon: ImageView
    private lateinit var batteryText: TextView
    private lateinit var shutterText: TextView
    private lateinit var apertureText: TextView
    private lateinit var exposureText: TextView
    private lateinit var isoIcon: ImageView
    private lateinit var isoLabel: TextView
    private lateinit var isoValue: TextView
    private lateinit var focusIcon: ImageView
    private lateinit var focusIndicator: TextView
    private lateinit var focusModeIcon: ImageView
    private lateinit var focusModeText: TextView
    private var leaving = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        surface = findViewById(R.id.preview_surface) as SurfaceView
        overlay = findViewById(R.id.crop_overlay) as CropOverlayView
        processingText = findViewById(R.id.processing) as TextView
        modeIcon = findViewById(R.id.mode_icon) as ImageView
        modeText = findViewById(R.id.mode_text) as TextView
        aspectChip = findViewById(R.id.aspect_chip) as TextView
        shotsIcon = findViewById(R.id.shots_icon) as ImageView
        shotsText = findViewById(R.id.shots_text) as TextView
        batteryIcon = findViewById(R.id.battery_icon) as ImageView
        batteryText = findViewById(R.id.battery_text) as TextView
        shutterText = findViewById(R.id.shutter_text) as TextView
        apertureText = findViewById(R.id.aperture_text) as TextView
        exposureText = findViewById(R.id.exposure_text) as TextView
        isoIcon = findViewById(R.id.iso_icon) as ImageView
        isoLabel = findViewById(R.id.iso_label) as TextView
        isoValue = findViewById(R.id.iso_value) as TextView
        focusIcon = findViewById(R.id.focus_icon) as ImageView
        focusIndicator = findViewById(R.id.focus_indicator) as TextView
        focusModeIcon = findViewById(R.id.focus_mode_icon) as ImageView
        focusModeText = findViewById(R.id.focus_mode_text) as TextView
        Trace.line("typefaces " + OsdTypeface.names().joinToString(","))
        val metrics = resources.displayMetrics
        Trace.line(
            "display ${metrics.widthPixels}x${metrics.heightPixels} density=${metrics.density}",
        )
        OsdTypeface.univers?.let { univers ->
            listOf(
                    modeText,
                    aspectChip,
                    shotsText,
                    batteryText,
                    shutterText,
                    apertureText,
                    exposureText,
                    isoLabel,
                    isoValue,
                    focusModeText,
                    processingText,
                )
                .forEach { it.typeface = univers }
        }
        // After the typeface, which measureText uses. The widest values each readout can show.
        val shutterWidth = shutterText.reserveWidthFor("1/8000", "BULB", "30\"", "0.5\"")
        val apertureWidth = apertureText.reserveWidthFor("F22", "F5.6", "F--")
        val exposureWidth = exposureText.reserveWidthFor("+5.0", "-5.0", "±0.0")
        val isoWidth = isoValue.reserveWidthFor("25600", "AUTO")
        batteryText.reserveWidthFor("100%")
        Trace.line(
            "readout widths shutter=$shutterWidth aperture=$apertureWidth " +
                "ev=$exposureWidth iso=$isoWidth",
        )
        viewModel = ShootingViewModel(applicationContext, ::recreateSurface)
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
        // Logged so a camera run shows what unmapped buttons (like AEL on the a7R) actually send.
        Trace.line("key $keyCode scan ${event.scanCode} unhandled")
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        if (event.repeatCount != 0) return super.onKeyUp(keyCode, event)
        val mapped = emulatorKey(keyCode) ?: CameraKeys.fromKeyEvent(event)
        if (viewModel.onKeyUp(mapped)) return true
        return super.onKeyUp(keyCode, event)
    }

    private fun emulatorKey(keyCode: Int): Int? {
        if (DeviceInfo.getInstance().isCamera) return null
        return when (keyCode) {
            KeyEvent.KEYCODE_A -> CameraKeys.ENTER
            KeyEvent.KEYCODE_S -> CameraKeys.DIAL_1_COUNTERCLOCKWISE
            KeyEvent.KEYCODE_D -> CameraKeys.DIAL_1_CLOCKWISE
            KeyEvent.KEYCODE_F -> CameraKeys.AEL
            KeyEvent.KEYCODE_W -> CameraKeys.WHEEL_COUNTERCLOCKWISE
            KeyEvent.KEYCODE_E -> CameraKeys.WHEEL_CLOCKWISE
            else -> null
        }
    }

    private fun render(state: ShootingState) {
        overlay.setAspect(state.aspect)
        overlay.setLiveView(state.liveView)
        processingText.visibility = if (state.shooting) View.VISIBLE else View.GONE

        showIcon(modeIcon, NativeIcons.modeDrawable(resources, state.exposureMode), modeText)
        modeText.text = state.mode

        aspectChip.text = state.aspect.label
        aspectChip.setTextColor(if (state.choosingRatio) 0xFFFFDD00.toInt() else Color.WHITE)

        showIcon(shotsIcon, NativeIcons.drawable(resources, NativeIcons.CARD))
        shotsText.text = state.shots
        showIcon(batteryIcon, NativeIcons.batteryDrawable(resources, state.batteryLevel))
        batteryText.text = state.battery

        shutterText.text = state.shutter
        apertureText.text = state.aperture
        exposureText.text = state.exposure
        showIcon(isoIcon, NativeIcons.drawable(resources, NativeIcons.ISO_LABEL), isoLabel)
        isoValue.text = state.iso

        renderFocus(state.focus)
        renderFocusMode(state.focusMode)
    }

    private fun renderFocusMode(mode: FocusMode) {
        if (mode == FocusMode.UNKNOWN) {
            focusModeIcon.visibility = View.GONE
            focusModeText.visibility = View.GONE
            return
        }
        showIcon(focusModeIcon, NativeIcons.focusModeDrawable(resources, mode), focusModeText)
        focusModeText.text = mode.label
    }

    private fun renderFocus(focus: Focus) {
        if (focus == Focus.IDLE) {
            focusIcon.visibility = View.GONE
            focusIndicator.visibility = View.GONE
            return
        }
        val color =
            when (focus) {
                Focus.FOCUSING -> Color.WHITE
                Focus.LOCKED -> 0xFF33DD33.toInt()
                Focus.FAILED -> 0xFFFF4444.toInt()
                else -> Color.WHITE
            }
        val dot = NativeIcons.drawable(resources, NativeIcons.AF_DOT)
        if (dot != null) {
            showIcon(focusIcon, dot, focusIndicator)
            focusIcon.setColorFilter(PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN))
        } else {
            focusIcon.visibility = View.GONE
            focusIndicator.visibility = View.VISIBLE
            focusIndicator.setTextColor(color)
        }
    }

    /** Shows the camera's own [drawable] in [icon] when there is one, else the text [fallback]. */
    private fun showIcon(icon: ImageView, drawable: Drawable?, fallback: View? = null) {
        icon.setImageDrawable(drawable)
        icon.visibility = if (drawable != null) View.VISIBLE else View.GONE
        fallback?.visibility = if (drawable != null) View.GONE else View.VISIBLE
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
