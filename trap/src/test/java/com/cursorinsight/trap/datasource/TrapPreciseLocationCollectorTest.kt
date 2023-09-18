package com.cursorinsight.trap.datasource

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.util.Log
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkObject
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
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TrapPreciseLocationCollectorTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test precise location`() {
        val activity = mockkClass(Activity::class)

        mockkObject(TrapBackgroundExecutor)
        every { TrapBackgroundExecutor.run(any()) } returns Unit
        every { TrapBackgroundExecutor.executor } returns mockk()

        var request: CapturingSlot<LocationRequest> = slot()
        var callback: CapturingSlot<LocationCallback> = slot()

        mockkStatic(LocationServices::class)
        every { LocationServices.getFusedLocationProviderClient(activity) } answers {
            val client = mockkClass(FusedLocationProviderClient::class)
            every { client.requestLocationUpdates(capture(request), any(), capture(callback)) } returns mockk()
            every { client.removeLocationUpdates(any(LocationCallback::class)) } returns mockk()
            client
        }

        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapPreciseLocationCollector(storage, TrapConfig())

        collector.start(activity)

        assert(request.isCaptured)
        assert(callback.isCaptured)

        val result = mockkClass(LocationResult::class)
        every { result.locations } answers {
            val loc = mockkClass(Location::class)
            every { loc.latitude } returns 11.0
            every { loc.longitude } returns  35.0
            every { loc.accuracy } returns 1.0F
            listOf(loc)
        }

        callback.captured.onLocationResult(result)

        Assertions.assertSame(storage.size, 1)
        val record1 = storage.first()
        assert(record1.getInt(0) == 109)
        assert(record1.getLong(1) > 0)
        assert(record1.getDouble(2) == 11.0)
        assert(record1.getDouble(3) == 35.0)
        assert(record1.getDouble(4) == 1.0)

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

        val result = TrapPreciseLocationCollector.checkPermissions(activity)
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
        TrapPreciseLocationCollector.requirePermissions(activity) { succeeded = true }
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
        TrapPreciseLocationCollector.requirePermissions(activity) { succeeded = true }
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