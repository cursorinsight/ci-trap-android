package com.cursorinsight.trap

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.view.Window
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.sensor.TrapGravityCollector
import com.cursorinsight.trap.transport.TrapReporter
import com.cursorinsight.trap.util.TrapTime
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
class TrapManagerTest {

    private lateinit var application: Application
    private var networkCallback: CapturingSlot<ConnectivityManager.NetworkCallback> = slot()

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0

        // We only test the TrapManager, no need to mock out everything
        mockkConstructor(TrapReporter::class)
        every { anyConstructed<TrapReporter>().start(any()) } returns Unit
        every { anyConstructed<TrapReporter>().stop() } returns Unit

        mockkConstructor(CircularFifoQueue::class)
        every { anyConstructed<CircularFifoQueue<JSONArray>>().add(any()) } returns true

        mockkConstructor(ConnectivityManager.NetworkCallback::class)
        every { anyConstructed<ConnectivityManager.NetworkCallback>().onCapabilitiesChanged(any(), any()) } returns Unit

        application = spyk(Application())
        every { application.cacheDir }  answers { File("/test/cache/dir") }
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit

        var intent = mockkClass(Intent::class)
        every { intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1) } returns 2
        every { intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) } returns 50
        every { intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1) } returns 100
        every { intent.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false) } returns false

        every { application.registerReceiver(any(), any()) } returns intent
        every { application.unregisterReceiver(any()) } returns Unit

        every { application.getSystemService("connectivity") } answers {
            val manager = mockkClass(ConnectivityManager::class)
            every { manager.registerNetworkCallback(any(), capture(networkCallback)) } returns Unit
            every { manager.unregisterNetworkCallback(any(ConnectivityManager.NetworkCallback::class)) } returns Unit
            manager
        }
        every {application.applicationContext} returns mockkClass(Context::class)

        val networkRequestBuilder = mockkClass(NetworkRequest.Builder::class)
        val networkRequest = mockkClass(NetworkRequest::class)
        mockkConstructor(NetworkRequest.Builder::class)

        every { anyConstructed<NetworkRequest.Builder>().addCapability(any()) } returns networkRequestBuilder
        every { networkRequestBuilder.addTransportType(any()) } returns networkRequestBuilder
        every { networkRequestBuilder.build() } returns networkRequest
        mockkStatic(NetworkRequest.Builder::class)

        mockkConstructor(IntentFilter::class)
        every { anyConstructed<IntentFilter>().addAction(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test getInstance returns the same instance with the same config`() {
        val config = TrapConfig()
        val instance1 = TrapManager.getInstance(application, config)
        val instance2 = TrapManager.getInstance(application, config)
        val instance3 = TrapManager.getInstance(application)

        assertSame(instance1, instance2)
        assertSame(instance1, instance3)
        verify { application invoke "registerActivityLifecycleCallbacks" withArguments listOf(instance1) }
    }

    @Test
    fun `test run adds collector and starts reporter`(@MockK activity: Activity) {
        val collector = mockkClass(TrapDatasource::class)
        every { collector.start(any(), any(), any()) } returns Unit
        every { collector.getName() } returns (TrapDatasource::class.qualifiedName ?: "")

        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf()

        val trapManager = TrapManager(application, config)

        trapManager.onActivityResumed(activity)
        var capabilities = mockkClass(NetworkCapabilities::class)
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        networkCallback.captured.onCapabilitiesChanged(
            mockkClass(Network::class),
            capabilities)

        trapManager.run(collector)

        verify(exactly = 1) { collector invoke "start" withArguments listOf(
                activity,
                config.defaultDataCollection,
                any<SynchronizedQueue<JSONArray>>())
        }
        verify(exactly = 2) { anyConstructed<TrapReporter>().start(false) }
        verify(exactly = 1) { anyConstructed<CircularFifoQueue<JSONArray>>().add(withArg {
            assert(it.getInt(0) == 130)
            assert(it.getLong(1) > 0)
        }) }
    }

    @Test
    fun `test halt stops collector and reporter`(@MockK activity: Activity) {
        val collector = mockkClass(TrapDatasource::class)
        every { collector.start(any(), any(), any()) } returns Unit
        every { collector.stop(any()) } returns Unit
        every { collector.getName() } returns (TrapDatasource::class.qualifiedName ?: "")

        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf()

        val trapManager = TrapManager.getInstance(application, config)
        trapManager.onActivityResumed(activity)
        trapManager.run(collector)
        trapManager.halt(collector)

        verify { collector invoke "stop" withArguments listOf(activity) }
        verify(exactly = 1) { anyConstructed<TrapReporter>().stop() }
    }

    @Test
    fun `test onActivityResumed runs all collectors`(@MockK activity: Activity) {
        mockkConstructor(TrapGravityCollector::class)
        every { anyConstructed<TrapGravityCollector>().start(activity, any(), any()) } returns Unit
        every { anyConstructed<TrapGravityCollector>().stop(activity) } returns Unit

        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf(TrapGravityCollector())

        val trapManager = TrapManager(application, config)
        trapManager.onActivityResumed(activity)

        var capabilities = mockkClass(NetworkCapabilities::class)
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        networkCallback.captured.onCapabilitiesChanged(
            mockkClass(Network::class),
            capabilities)

        verify(exactly = 1) { anyConstructed<TrapGravityCollector>().start(
            activity,
            config.defaultDataCollection,
            any()) }
        verify(exactly = 1) { anyConstructed<TrapReporter>().start(false) }
    }

    @Test
    fun `test onActivityPaused halts all collectors`(@MockK activity: Activity) {
        mockkConstructor(TrapGravityCollector::class)
        every { anyConstructed<TrapGravityCollector>().start(activity, any(), any()) } returns Unit
        every { anyConstructed<TrapGravityCollector>().stop(activity) } returns Unit

        every { activity.packageManager } answers {
            val packageManager = mockkClass(PackageManager::class)
            every { packageManager.hasSystemFeature(any()) } returns true
            packageManager
        }
        every { activity.getSystemService(Context.SENSOR_SERVICE) } answers {
            val sensorManager = mockkClass(SensorManager::class)
            every { sensorManager.unregisterListener(any<SensorEventListener>()) } returns Unit
            sensorManager
        }

        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf(TrapGravityCollector())

        val trapManager = TrapManager(application, config)
        trapManager.onActivityResumed(activity)

        var capabilities = mockkClass(NetworkCapabilities::class)
        every { capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } returns true
        networkCallback.captured.onCapabilitiesChanged(
            mockkClass(Network::class),
            capabilities)

        trapManager.onActivityPaused(activity)

        verify(exactly = 1) { anyConstructed<TrapGravityCollector>().stop(activity) }
        verify(exactly = 1) { anyConstructed<TrapReporter>().stop() }
        verify(exactly = 1) { anyConstructed<CircularFifoQueue<JSONArray>>().add(withArg {
            assert(it.getInt(0) == 131)
            assert(it.getLong(1) > 0)
        }) }
    }

    @Test
    fun `test window callback is registered`() {
        val window = mockkClass(Window::class)
        every { window.callback } answers { mockkClass(Window.Callback::class) }
        every { window.callback = any() } answers { }

        val activity = mockkClass(Activity::class)
        every { activity.window } answers { window }

        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf()

        val trapManager = TrapManager(application, config)
        trapManager.onActivityCreated(activity, null)

        verify { window.callback }
    }

    @Test
    fun `test additional methods`(@MockK activity: Activity) {
        val config = TrapConfig()
        config.defaultDataCollection.collectors = mutableListOf()

        val trapManager = TrapManager(application, config)

        trapManager.onActivityStopped(activity)
        trapManager.onActivityStarted(activity)
        trapManager.onActivitySaveInstanceState(activity, Bundle())
        trapManager.onActivityDestroyed(activity)
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
