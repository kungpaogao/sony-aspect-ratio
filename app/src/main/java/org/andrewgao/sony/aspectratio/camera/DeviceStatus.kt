package org.andrewgao.sony.aspectratio.camera

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.github.ma1co.openmemories.framework.MediaManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Battery percent and shots remaining from system broadcasts and MediaManager. */
class DeviceStatus {
    private val mutableBattery = MutableStateFlow(-1)
    val battery: StateFlow<Int> = mutableBattery

    private val mutableShots = MutableStateFlow(-1)
    val shots: StateFlow<Int> = mutableShots

    private var context: Context? = null
    private var batteryReceiver: BroadcastReceiver? = null
    private var mediaManager: MediaManager? = null

    fun start(ctxt: Context) {
        context = ctxt
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(ctx: Context, intent: Intent) {
                    mutableBattery.value = batteryPercent(intent)
                }
            }
        val sticky = ctxt.registerReceiver(receiver, filter)
        if (sticky != null) {
            mutableBattery.value = batteryPercent(sticky)
        }
        batteryReceiver = receiver

        val manager = MediaManager.create(ctxt)
        mediaManager = manager
        mutableShots.value = manager.availableShots
        manager.setAvailableShotsListener { count -> mutableShots.value = count }
    }

    fun stop() {
        val ctxt = context
        if (batteryReceiver != null && ctxt != null) {
            ctxt.unregisterReceiver(batteryReceiver)
        }
        batteryReceiver = null
        mediaManager?.setAvailableShotsListener(null)
        mediaManager = null
        context = null
    }

    /** -1 when the broadcast doesn't carry a level. */
    private fun batteryPercent(intent: Intent): Int {
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level < 0 || scale <= 0) -1 else level * 100 / scale
    }
}
