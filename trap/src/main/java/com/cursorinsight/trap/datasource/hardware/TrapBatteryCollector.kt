package com.cursorinsight.trap.datasource.hardware

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BATTERY_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.sensor.TrapAccelerometerCollector
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.json.JSONObject

/**
 * Monitors for battery status then reports them
 * as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapBatteryCollector(
    private val storage: SynchronizedQueue<JSONArray>
): TrapDatasource {
    /**
     * The Trap event type for battery status
     */
    private val batteryEventType = 133

    /**
     * The system service for battery.
     */
    private var batteryManager: BatteryManager? = null

    /**
     * If true, then we have a registered location handler we need to
     * deregister.
     */
    private var registered = false

    /**
     * The callback for the wifi service.
     */
    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                processBatteryResult(intent)
            } catch (ex: Exception) {
                Log.e(
                    TrapBatteryCollector::class.simpleName,
                    "Processing battery event failed",
                    ex
                )
            }
        }
    }

    /**
     * Process the scan results
     */
    private fun processBatteryResult(intent : Intent) {
        var batteryLevel = batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
        var batteryStatus: Int = -1

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            batteryStatus = when(batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS)) {
                BatteryManager.BATTERY_STATUS_DISCHARGING -> 0
                BatteryManager.BATTERY_STATUS_NOT_CHARGING -> 0
                BatteryManager.BATTERY_STATUS_CHARGING -> 1
                BatteryManager.BATTERY_STATUS_FULL -> 2
                BatteryManager.BATTERY_STATUS_UNKNOWN -> -1
                else -> -1
            }
        }

        with(JSONArray()) {
            put(batteryEventType)
            put(TrapTime.getCurrentTime())
            put(batteryLevel)
            put(batteryStatus)
            put(with(JSONObject()) {
                put(
                    "capacity",
                    batteryManager?.getIntProperty(
                        BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    put("chargeTimeRemaining",
                        batteryManager?.computeChargeTimeRemaining())
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    put("low",
                        intent.getBooleanExtra(
                            BatteryManager.EXTRA_BATTERY_LOW,
                            false))
                }
                put("health",
                    intent.getIntExtra(BatteryManager.EXTRA_HEALTH, -99))
                put("chargeType",
                    intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -99))
                put("voltage",
                    intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, -99))
                put("temperature",
                    intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -99))
                this
            })
            this
        }.let {
            storage.add(it)
        }
    }

    override fun start(activity: Activity, config: TrapConfig.DataCollection) {
        batteryManager = activity.getSystemService(BATTERY_SERVICE) as BatteryManager
        activity.registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_OKAY))
        registered = true
    }

    override fun stop(activity: Activity) {
        if (registered) {
            activity.unregisterReceiver(batteryReceiver)
        }
    }
}
