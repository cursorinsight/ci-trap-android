package com.cursorinsight.trap.transport

import android.util.Log
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.java_websocket.client.WebSocketClient
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.net.URI
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture

@ExtendWith(MockKExtension::class)
class TrapWebsocketTransportTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test websocket`() {
        var command: CapturingSlot<Runnable> = slot()
        val fut = mockkClass(ScheduledFuture::class)
        every { fut.cancel(any()) } returns true
        mockkStatic(Executors::class)
        every { Executors.newScheduledThreadPool(any()) } answers {
            val service = mockkClass(ScheduledExecutorService::class)
            every { service.scheduleWithFixedDelay(capture(command), any(), any(), any())} returns fut
            service
        }

        var msg: CapturingSlot<String> = slot()
        mockkConstructor(TrapWebsocketTransport.WebSocket::class)
        every { anyConstructed<TrapWebsocketTransport.WebSocket>().send(capture(msg)) } returns Unit

        val transport = TrapWebsocketTransport()
        transport.start(URI.create("ws://localhost"))

        transport.send("[[999]]")
        assert(msg.isCaptured)

        transport.stop()
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
            every { Log.w(any(String::class), any(String::class), any()) } returns 0
        }
    }
}