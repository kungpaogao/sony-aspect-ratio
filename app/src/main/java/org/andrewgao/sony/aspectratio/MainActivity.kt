package org.andrewgao.sony.aspectratio

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.widget.TextView
import org.andrewgao.sony.aspectratio.camera.CameraSession
import org.andrewgao.sony.aspectratio.camera.LensReading

class MainActivity : Activity() {
    private val session = CameraSession()
    private var holder: SurfaceHolder? = null
    private var surfaceReady = false
    private var resumed = false

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

    private fun startPreview() {
        val surfaceHolder = holder
        if (!resumed || !surfaceReady || surfaceHolder == null) return
        showLens(session.open(surfaceHolder))
    }

    private fun showLens(reading: LensReading) {
        val text = findViewById(R.id.lens_info) as TextView
        if (!reading.available) {
            text.text = getString(R.string.camera_unavailable)
            return
        }
        val lines = StringBuilder()
        val name = reading.name
        lines.append(if (name.isNullOrBlank()) getString(R.string.lens_missing) else name)
        val mount = reading.mount
        if (!mount.isNullOrBlank()) {
            lines.append('\n').append(mount)
        }
        val focal = reading.focal
        if (!focal.isNullOrBlank()) {
            lines.append('\n').append(focal)
        }
        text.text = lines.toString()
    }
}
