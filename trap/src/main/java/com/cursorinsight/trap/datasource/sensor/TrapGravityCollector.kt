package com.cursorinsight.trap.datasource.sensor

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Monitors for gravity sensor updates and sends them as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapGravityCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
): TrapDatasource {
    private val gravityEventType = 105

    private val handler = object: SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            val frame = JSONArray()
            frame.put(gravityEventType)
            frame.put(TrapTime.normalizeNanosecondTime(event?.timestamp ?: 0))
            frame.put(event?.values?.get(0))
            frame.put(event?.values?.get(1))
            frame.put(event?.values?.get(2))
            storage.add(frame)
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) { }
    }

    override fun start(activity: Activity) {
        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)
            sensor?.let {
                sensorManager.registerListener(handler, it, SensorManager.SENSOR_DELAY_GAME)
            }
        }
    }

    override fun stop(activity: Activity) {
        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_GYROSCOPE)) {
            val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(handler)
        }
    }
}