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
import com.cursorinsight.trap.util.TrapLogger
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Monitors for accelerometer sensor updates and sends them as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapAccelerometerCollector() : TrapDatasource {
    private val accelerometerEventType = 103
    private lateinit var logger : TrapLogger

    private var storage: SynchronizedQueue<JSONArray>? = null

    private val handler = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            try {
                val frame = JSONArray()
                frame.put(accelerometerEventType)
                frame.put(TrapTime.normalizeRealTimeNanosecond(event?.timestamp ?: 0))
                frame.put(event?.values?.get(0))
                frame.put(event?.values?.get(1))
                frame.put(event?.values?.get(2))
                storage?.add(frame)
            } catch (ex: Exception) {
                logger.logException(
                    TrapAccelerometerCollector::class.simpleName,
                    "Processing sensor event failed",
                    ex
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun start(
        activity: Activity,
        config: TrapConfig.DataCollection,
        storage: SynchronizedQueue<JSONArray>) {
        this.storage = storage
        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            logger = TrapLogger(config.maxNumberOfLogMessagesPerMinute)
            val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            sensor?.let {
                sensorManager.registerListener(
                    handler,
                    it,
                    config.accelerationSamplingPeriodMs * 1000,
                    config.accelerationMaxReportLatencyMs * 1000)
            }
        }
    }

    override fun stop(activity: Activity) {
        if (activity.packageManager.hasSystemFeature(PackageManager.FEATURE_SENSOR_ACCELEROMETER)) {
            val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            sensorManager.unregisterListener(handler)
        }
    }
}