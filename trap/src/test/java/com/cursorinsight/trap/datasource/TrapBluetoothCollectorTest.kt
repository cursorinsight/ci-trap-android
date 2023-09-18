package com.cursorinsight.trap.datasource

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Paint.Cap
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
import org.awaitility.Awaitility.await
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TrapBluetoothCollectorTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test Bluetooth event`() {
        mockkStatic(ContextCompat::class)
        every {
            ContextCompat.checkSelfPermission(
                any(),
                any()
            )
        } returns PackageManager.PERMISSION_GRANTED

        var receiver: CapturingSlot<BroadcastReceiver> = slot()
        var activity = mockkClass(Activity::class)
        every {
            activity.registerReceiver(
                capture(receiver),
                any(IntentFilter::class)
            )
        } returns mockk()
        every { activity.unregisterReceiver(any(BroadcastReceiver::class)) } returns Unit
        every { activity.packageManager } answers {
            val packageManager = mockkClass(PackageManager::class)
            every { packageManager.hasSystemFeature(any()) } returns true
            packageManager
        }
        every { activity.getSystemService(any(String::class)) } answers {
            val manager = mockkClass(BluetoothManager::class)
            every { manager.adapter } answers {
                val adapter = mockkClass(BluetoothAdapter::class)
                every { adapter.startDiscovery() } returns true
                every { adapter.isDiscovering } returns true
                every { adapter.cancelDiscovery() } returns true
                every { adapter.bondedDevices } answers {
                    val device = mockkClass(BluetoothDevice::class)
                    every { device.name } returns "Test Device"
                    every { device.address } returns "01:23:45:67:89:AB:CD:EF"
                    mutableSetOf<BluetoothDevice>(device)
                }
                adapter
            }
            manager
        }

        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapBluetoothCollector(storage, TrapConfig())

        collector.start(activity)
        assertSame(storage.size, 1)
        val record1 = storage.first()
        assert(record1.getInt(0) == 108)
        assert(record1.getLong(1) > 0)
        val record1subarray = record1.getJSONArray(2)
        val record1device = record1subarray.getJSONArray(0)
        assert(record1device.getString(0) == "Test Device")
        assert(record1device.getString(1) == "01:23:45:67:89:AB:CD:EF")
        assertSame(record1device.getInt(2), 2)
        assert(receiver.isCaptured)

        val intent = mockkClass(Intent::class)
        every { intent.action } returns BluetoothDevice.ACTION_FOUND
        every { intent.getParcelableExtra<BluetoothDevice>(any(String::class)) } answers {
            val device = mockkClass(BluetoothDevice::class)
            every { device.name } returns "Found Device"
            every { device.address } returns "FE:DC:BA:98:76:54:32:10"
            every { device.bondState } returns BluetoothDevice.BOND_BONDED
            device
        }
        receiver.captured.onReceive(mockk(), intent)
        assertSame(storage.size, 2)
        val record2 = storage.last()
        assert(record2.getInt(0) == 108)
        assert(record2.getLong(1) > 0)
        val record2subarray = record2.getJSONArray(2)
        val record2device = record2subarray.getJSONArray(0)
        assert(record2device.getString(0) == "Found Device")
        assert(record2device.getString(1) == "FE:DC:BA:98:76:54:32:10")
        assertSame(record2device.getInt(2), 3)
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

        val result = TrapBluetoothCollector.checkPermissions(activity)
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
        TrapBluetoothCollector.requirePermissions(activity) { succeeded = true }
        await().until() { succeeded }
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
        TrapBluetoothCollector.requirePermissions(activity) { succeeded = true }
        assert(receiver.isCaptured)
        receiver.captured.onReceive(null, null)
        await().until() { succeeded }
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