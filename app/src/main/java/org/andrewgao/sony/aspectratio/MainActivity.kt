package org.andrewgao.sony.aspectratio

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val surface = findViewById(R.id.preview_surface) as SurfaceView
        // Required on API 10, which is what the camera emulator runs.
        @Suppress("DEPRECATION")
        surface.holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
    }
}
