package com.cursorinsight.trap.datasource

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.hardware.TrapBatteryCollector
import com.cursorinsight.trap.testUtils.Utils
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.awaitility.Awaitility
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.lang.reflect.Modifier

@ExtendWith(MockKExtension::class)
class TrapBatteryCollectorTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0

        Utils.setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), 28)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        Utils.setFinalStatic(Build.VERSION::class.java.getField("SDK_INT"), 0)
    }

    @Test
    fun `test battery`() {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapBatteryCollector()

        var batteryReceiver: CapturingSlot<BroadcastReceiver> = slot()
        var activity = mockkClass(Activity::class)
        every {
            activity.registerReceiver(
                capture(batteryReceiver),
                any(IntentFilter::class)
            )
        } returns mockk()
        every { activity.unregisterReceiver(any()) } returns Unit

        every { activity.getSystemService(Context.BATTERY_SERVICE) } answers {
            val manager = mockkClass(BatteryManager::class)
            every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) } returns 10
            every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_STATUS) } returns 2
            every { manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) } returns 30
            every { manager.computeChargeTimeRemaining() } returns 45
            manager
        }

        var intent = mockkClass(Intent::class)
        every { intent.getIntExtra(BatteryManager.EXTRA_HEALTH, any()) } returns 1
        every { intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, any()) } returns 2
        every { intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, any()) } returns 3
        every { intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, any()) } returns 4
        every { intent.getBooleanExtra(any(), any()) } returns false
        collector.start(activity, TrapConfig.DataCollection(), storage)

        assert(batteryReceiver.isCaptured)
        batteryReceiver.captured.onReceive(mockk(), intent)

        Assertions.assertSame(storage.size, 1)
        val record1 = storage.first()

        assert(record1.getInt(0) == 133)
        assert(record1.getLong(1) > 0)
        assert(record1.getInt(2) == 10)
        assert(record1.getInt(3) == 1)

        val record1Object = record1.getJSONObject(4)
        assert(record1Object.getInt("capacity") == 30)
        assert(record1Object.getInt("chargeTimeRemaining") == 45)
        assert(record1Object.getInt("health") == 1)
        assert(record1Object.getInt("chargeType") == 2)
        assert(record1Object.getInt("voltage") == 3)
        assert(record1Object.getInt("temperature") == 4)

        collector.stop(activity)
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