package com.cursorinsight.trap.datasource.sensor

import android.app.Activity
import android.content.Context
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
 * Monitors for magnetometer sensor updates and sends them as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapMagnetometerCollector(
    private val storage: SynchronizedQueue<JSONArray>
) : TrapDatasource {
    private val magneticEventType = 106
    private lateinit var logger : TrapLogger

    private val handler = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent?) {
            try {
                val frame = JSONArray()
                frame.put(magneticEventType)
                frame.put(TrapTime.normalizeRealTimeNanosecond(event?.timestamp ?: 0))
                frame.put(event?.values?.get(0))
                frame.put(event?.values?.get(1))
                frame.put(event?.values?.get(2))
                storage.add(frame)
            } catch (ex: Exception) {
                logger.logException(
                    TrapMagnetometerCollector::class.simpleName,
                    "Processing sensor event failed",
                    ex
                )
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }

    override fun start(activity: Activity, config: TrapConfig.DataCollection) {
        logger = TrapLogger(config.maxNumberOfLogMessagesPerMinute)
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensor?.let {
            sensorManager.registerListener(
                handler,
                it,
                config.magnetometerSamplingPeriodMs * 1000,
                config.magnetometerMaxReportLatencyMs * 1000)
        }
    }

    override fun stop(activity: Activity) {
        val sensorManager = activity.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        sensorManager.unregisterListener(handler)
    }
}