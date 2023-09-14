package com.cursorinsight.trap.datasource

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.SystemClock
import android.util.Log
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
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

@ExtendWith(MockKExtension::class)
class TrapWifiCollectorTest {

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test wifi`() {
        var wifiReceiver: CapturingSlot<BroadcastReceiver> = slot()
        var activity = mockkClass(Activity::class)
        every {
            activity.registerReceiver(
                capture(wifiReceiver),
                any(IntentFilter::class)
            )
        } returns mockk()
        every { activity.unregisterReceiver(any()) } returns Unit
        every { activity.getSystemService("wifi") } answers {
            val manager = mockkClass(WifiManager::class)
            every { manager.startScan() } returns true
            every { manager.scanResults } answers {
                val network = mockkClass(ScanResult::class)
                network.SSID = "Test Net"
                network.BSSID = "01:23:45:67:89:AB:CD:EF"
                listOf(network)
            }
            manager
        }
        var connectivityReceiver: CapturingSlot<ConnectivityManager.NetworkCallback> = slot()
        every { activity.getSystemService("connectivity") } answers {
            val manager = mockkClass(ConnectivityManager::class)
            every { manager.registerDefaultNetworkCallback(capture(connectivityReceiver)) } returns Unit
            every { manager.unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback::class)) } returns Unit

            manager
        }

        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapWiFiCollector(storage)

        collector.start(activity, TrapConfig.DataCollection())
        Assertions.assertSame(storage.size, 1)
        val record1 = storage.first()
        assert(record1.getInt(0) == 107)
        assert(record1.getLong(1) > 0)
        val record1subarray = record1.getJSONArray(2)
        val record1data = record1subarray.getJSONArray(0)
        assert(record1data.getString(0) == "Test Net")
        assert(record1data.getString(1) == "01:23:45:67:89:AB:CD:EF")

        assert(wifiReceiver.isCaptured)
        wifiReceiver.captured.onReceive(mockk(), mockk())
        Assertions.assertSame(storage.size, 2)

        assert(connectivityReceiver.isCaptured)
        val capabilities = mockkClass(NetworkCapabilities::class)
        every { capabilities.hasTransport(any()) } returns true
        connectivityReceiver.captured.onCapabilitiesChanged(mockk(), capabilities)
        Assertions.assertSame(storage.size, 3)

        collector.stop(activity)
    }

    @Test
    fun `test permission check`(@MockK activity: Activity) {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                any(), any()
            )
        } returns PackageManager.PERMISSION_GRANTED

        val result = TrapWiFiCollector.checkPermissions(activity)
        assert(result)
    }

    @Test
    fun `test require permission when permission is already given`(@MockK activity: Activity) {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                activity, any()
            )
        } returns PackageManager.PERMISSION_GRANTED

        var succeeded = false
        TrapWiFiCollector.requirePermissions(activity) { succeeded = true }
        Awaitility.await().until() { succeeded }
        assert(succeeded)
    }

    @Test
    fun `test require permission when not yet given`() {
        var receiver: CapturingSlot<BroadcastReceiver> = slot()

        val activity = mockkClass(Activity::class)
        every { activity.registerReceiver(capture(receiver), any()) } returns mockk()
        every { activity.startActivity(any()) } returns Unit

        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                activity, any()
            )
        } returns PackageManager.PERMISSION_DENIED

        mockkConstructor(Intent::class)
        every {
            anyConstructed<Intent>().putExtra(
                any(String::class),
                any(Int::class)
            )
        } returns mockk()

        var succeeded = false
        TrapWiFiCollector.requirePermissions(activity) { succeeded = true }
        assert(receiver.isCaptured)
        receiver.captured.onReceive(null, null)
        Awaitility.await().until() { succeeded }
        assert(succeeded)
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