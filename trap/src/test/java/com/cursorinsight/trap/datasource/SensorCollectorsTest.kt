package com.cursorinsight.trap.datasource

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.SystemClock
import android.util.Log
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.sensor.TrapAccelerometerCollector
import com.cursorinsight.trap.datasource.sensor.TrapGravityCollector
import com.cursorinsight.trap.datasource.sensor.TrapGyroscopeCollector
import com.cursorinsight.trap.datasource.sensor.TrapMagnetometerCollector
import com.cursorinsight.trap.util.TrapTime
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SensorCollectorsTest {

    private lateinit var handler: CapturingSlot<SensorEventListener>
    private lateinit var unhandler: CapturingSlot<SensorEventListener>

    private var activity: Activity = run {
        val activity = mockkClass(Activity::class)
        every { activity.packageManager } answers {
            val packageManager = mockkClass(PackageManager::class)
            every { packageManager.hasSystemFeature(any()) } returns true
            packageManager
        }
        every { activity.getSystemService(Context.SENSOR_SERVICE) } answers {
            val sensorManager = mockkClass(SensorManager::class)
            every { sensorManager.getDefaultSensor(any()) } answers {
                val sensor = mockkClass(Sensor::class)
                sensor
            }
            every {
                sensorManager.registerListener(
                    capture(this@SensorCollectorsTest.handler),
                    any(),
                    any<Int>(),
                    any<Int>()
                )
            } returns true
            every {
                sensorManager.unregisterListener(
                    capture(this@SensorCollectorsTest.unhandler)
                )
            } returns Unit

            sensorManager
        }
        activity
    }

    @BeforeEach
    fun setUp() {
        handler = slot()
        unhandler = slot()

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun getEvent(timestamp: Long, x: Float, y: Float, z: Float): SensorEvent {
        val event = mockkClass(SensorEvent::class)
        val timestampField = event.javaClass.getField("timestamp")
        timestampField.set(event, timestamp)
        val valuesField = event.javaClass.getField("values")
        valuesField.set(event, floatArrayOf(x, y, z))

        return event
    }

    @Test
    fun `test accelerometer data is collected`() {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapAccelerometerCollector(storage, TrapConfig())
        collector.start(activity)
        assert(handler.isCaptured)

        handler.captured.onSensorChanged(getEvent(-1L, 1.0F, 2.0F, -2.0F))
        assert(storage.size == 1)
        val el = storage.elementAt(0)
        assert(el.getInt(0) == 103)
        assert(el.getLong(1) == TrapTime.normalizeRealTimeNanosecond(1L))
        assert(el.getDouble(2) == 1.0)
        assert(el.getDouble(3) == 2.0)
        assert(el.getDouble(4) == -2.0)

        handler.captured.onSensorChanged(getEvent(-3L, 3.0F, 4.0F, -4.0F))
        assert(storage.size == 2)
        val el2 = storage.elementAt(1)
        assert(el2.getInt(0) == 103)
        assert(el2.getLong(1) == TrapTime.normalizeRealTimeNanosecond(3L))
        assert(el2.getDouble(2) == 3.0)
        assert(el2.getDouble(3) == 4.0)
        assert(el2.getDouble(4) == -4.0)

        handler.captured.onAccuracyChanged(mockk(), 0)

        collector.stop(activity)
        assert(unhandler.isCaptured)
    }

    @Test
    fun `test gravity data is collected`() {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapGravityCollector(storage, TrapConfig())
        collector.start(activity)
        assert(handler.isCaptured)

        handler.captured.onSensorChanged(getEvent(-1L, 1.0F, 2.0F, -2.0F))
        assert(storage.size == 1)
        val el = storage.elementAt(0)
        assert(el.getInt(0) == 105)
        assert(el.getLong(1) == TrapTime.normalizeRealTimeNanosecond(1L))
        assert(el.getDouble(2) == 1.0)
        assert(el.getDouble(3) == 2.0)
        assert(el.getDouble(4) == -2.0)

        handler.captured.onSensorChanged(getEvent(-3L, 3.0F, 4.0F, -4.0F))
        assert(storage.size == 2)
        val el2 = storage.elementAt(1)
        assert(el2.getInt(0) == 105)
        assert(el2.getLong(1) == TrapTime.normalizeRealTimeNanosecond(3L))
        assert(el2.getDouble(2) == 3.0)
        assert(el2.getDouble(3) == 4.0)
        assert(el2.getDouble(4) == -4.0)

        handler.captured.onAccuracyChanged(mockk(), 0)

        collector.stop(activity)
        assert(unhandler.isCaptured)
    }

    @Test
    fun `test gyroscope data is collected`() {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapGyroscopeCollector(storage, TrapConfig())
        collector.start(activity)
        assert(handler.isCaptured)

        handler.captured.onSensorChanged(getEvent(-1L, 1.0F, 2.0F, -2.0F))
        assert(storage.size == 1)
        val el = storage.elementAt(0)
        assert(el.getInt(0) == 104)
        assert(el.getLong(1) == TrapTime.normalizeRealTimeNanosecond(1L))
        assert(el.getDouble(2) == 1.0)
        assert(el.getDouble(3) == 2.0)
        assert(el.getDouble(4) == -2.0)

        handler.captured.onSensorChanged(getEvent(-3L, 3.0F, 4.0F, -4.0F))
        assert(storage.size == 2)
        val el2 = storage.elementAt(1)
        assert(el2.getInt(0) == 104)
        assert(el2.getLong(1) == TrapTime.normalizeRealTimeNanosecond(3L))
        assert(el2.getDouble(2) == 3.0)
        assert(el2.getDouble(3) == 4.0)
        assert(el2.getDouble(4) == -4.0)

        handler.captured.onAccuracyChanged(mockk(), 0)

        collector.stop(activity)
        assert(unhandler.isCaptured)
    }

    @Test
    fun `test magnetometer data is collected`() {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapMagnetometerCollector(storage, TrapConfig())
        collector.start(activity)
        assert(handler.isCaptured)

        handler.captured.onSensorChanged(getEvent(-1L, 1.0F, 2.0F, -2.0F))
        assert(storage.size == 1)
        val el = storage.elementAt(0)
        assert(el.getInt(0) == 106)
        assert(el.getLong(1) == TrapTime.normalizeRealTimeNanosecond(1L))
        assert(el.getDouble(2) == 1.0)
        assert(el.getDouble(3) == 2.0)
        assert(el.getDouble(4) == -2.0)

        handler.captured.onSensorChanged(getEvent(-3L, 3.0F, 4.0F, -4.0F))
        assert(storage.size == 2)
        val el2 = storage.elementAt(1)
        assert(el2.getInt(0) == 106)
        assert(el2.getLong(1) == TrapTime.normalizeRealTimeNanosecond(3L))
        assert(el2.getDouble(2) == 3.0)
        assert(el2.getDouble(3) == 4.0)
        assert(el2.getDouble(4) == -4.0)

        handler.captured.onAccuracyChanged(mockk(), 0)

        collector.stop(activity)
        assert(unhandler.isCaptured)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
        }
    }
}