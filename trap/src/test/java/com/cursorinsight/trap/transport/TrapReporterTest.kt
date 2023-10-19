package com.cursorinsight.trap.transport

import android.content.Context
import android.os.SystemClock
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapFileCache
import io.mockk.CapturingSlot
import io.mockk.every
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
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

@ExtendWith(MockKExtension::class)
class TrapReporterTest {
    private val tempDir = System.getProperty("java.io.tmpdir")

    @BeforeEach
    fun setUp() {
        val cache = TrapFileCache(File(tempDir), 128)
        cache.clear()

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test reporter`() {
        var command: CapturingSlot<Runnable> = slot()
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val context = mockkClass(Context::class)
        every { context.cacheDir } returns File(tempDir)
        val reporter = TrapReporter(TrapConfig(), context, storage)

        val fut = mockkClass(ScheduledFuture::class)
        every { fut.cancel(any()) } returns true
        mockkStatic(Executors::class)
        every { Executors.newScheduledThreadPool(any()) } answers {
            val service = mockkClass(ScheduledExecutorService::class)
            every { service.scheduleWithFixedDelay(capture(command), any(), any(), any())} returns fut
            service
        }

        var msg: CapturingSlot<String> = slot()
        mockkConstructor(TrapHttpTransport::class)
        every { anyConstructed<TrapHttpTransport>().start(any(), any()) } returns Unit
        every { anyConstructed<TrapHttpTransport>().stop() } returns Unit
        every { anyConstructed<TrapHttpTransport>().send(capture(msg), any()) } returns Unit

        reporter.start(false)

        assert(command.isCaptured)
        storage.add(with(JSONArray()) {
            put(999)
            this
        })

        command.captured.run()

        assert(msg.isCaptured)

        reporter.stop()
    }
}