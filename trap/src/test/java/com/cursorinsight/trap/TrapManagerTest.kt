package com.cursorinsight.trap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.Window
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.sensor.TrapGravityCollector
import com.cursorinsight.trap.transport.TrapReporter
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
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

    @BeforeEach
    fun setUp() {
        // We only test the TrapManager, no need to mock out everything
        mockkConstructor(TrapReporter::class)
        every { anyConstructed<TrapReporter>().start() } returns Unit
        every { anyConstructed<TrapReporter>().stop() } returns Unit

        application = spyk(Application())
        every { application.cacheDir }  answers { File("/test/cache/dir") }
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test incorrect data collector is reported`() {
        mockkConstructor(TrapDatasource::class)
        val config = TrapConfig()
        config.collectors = mutableListOf(TrapDatasource::class)

        TrapManager(application, config)

        verify(exactly = 0) { anyConstructed<TrapDatasource>().start(any()) }
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
        every { collector.start(any()) } returns Unit

        val config = TrapConfig()
        config.collectors = mutableListOf()

        val trapManager = TrapManager(application, config)

        trapManager.onActivityResumed(activity)
        trapManager.run(collector)

        verify(exactly = 1) { collector invoke "start" withArguments listOf(activity) }
        verify(exactly = 2) { anyConstructed<TrapReporter>().start() }
    }

    @Test
    fun `test halt stops collector and reporter`(@MockK activity: Activity) {
        val collector = mockkClass(TrapDatasource::class)
        every { collector.start(any()) } returns Unit
        every { collector.stop(any()) } returns Unit

        val config = TrapConfig()
        config.collectors = mutableListOf()

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
        every { anyConstructed<TrapGravityCollector>().start(activity) } returns Unit
        every { anyConstructed<TrapGravityCollector>().stop(activity) } returns Unit

        val config = TrapConfig()
        config.collectors = mutableListOf(TrapGravityCollector::class)

        val trapManager = TrapManager(application, config)
        trapManager.onActivityResumed(activity)

        verify(exactly = 1) { anyConstructed<TrapGravityCollector>().start(activity) }
        verify(exactly = 1) { anyConstructed<TrapReporter>().start() }
    }

    @Test
    fun `test onActivityPaused halts all collectors`(@MockK activity: Activity) {
        mockkConstructor(TrapGravityCollector::class)
        every { anyConstructed<TrapGravityCollector>().start(activity) } returns Unit
        every { anyConstructed<TrapGravityCollector>().stop(activity) } returns Unit

        val config = TrapConfig()
        config.collectors = mutableListOf(TrapGravityCollector::class)

        val trapManager = TrapManager(application, config)
        trapManager.onActivityPaused(activity)

        verify(exactly = 1) { anyConstructed<TrapGravityCollector>().stop(activity) }
        verify(exactly = 1) { anyConstructed<TrapReporter>().stop() }
    }

    @Test
    fun `test window callback is registered`() {
        val window = mockkClass(Window::class)
        every { window.callback } answers { mockkClass(Window.Callback::class) }
        every { window.callback = any() } answers { }

        val activity = mockkClass(Activity::class)
        every { activity.window } answers { window }

        val config = TrapConfig()
        config.collectors = mutableListOf()

        val trapManager = TrapManager(application, config)
        trapManager.onActivityCreated(activity, null)

        verify { window.callback }
    }

    @Test
    fun `test additional methods`(@MockK activity: Activity) {
        val config = TrapConfig()
        config.collectors = mutableListOf()

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
