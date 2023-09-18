package com.cursorinsight.trap.transport

import com.cursorinsight.trap.util.TrapFileCache
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkClass
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.net.URI

@ExtendWith(MockKExtension::class)
class TrapCachedTransportTest {
    private val tempDir = System.getProperty("java.io.tmpdir")

    @BeforeEach
    fun setUp() {
        val cache = TrapFileCache(File(tempDir), 128)
        cache.clear()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test cached transport`() {
        val underlying = mockkClass(TrapTransport::class)
        every { underlying.start(any()) } returns Unit
        every { underlying.stop() } returns Unit
        every { underlying.send(any(String::class))} returns Unit

        val transport = TrapCachedTransport(File(tempDir), 128, underlying)
        transport.start(URI.create("http://localhost"))
        verify(exactly = 1) { underlying.start(any()) }
        transport.stop()
        verify(exactly = 1) { underlying.stop() }
        transport.send("[[999,12345678]]")
        verify(exactly = 1) { transport.send("[[999,12345678]]") }
    }

    @Test
    fun `test cached transport with cached results`() {
        var msg: CapturingSlot<String> = slot()
        val underlying = mockkClass(TrapTransport::class)
        every { underlying.start(any()) } returns Unit
        every { underlying.stop() } returns Unit
        every { underlying.send(capture(msg))} throws TrapTransportException() andThen Unit

        val transport = TrapCachedTransport(File(tempDir), 128, underlying)
        transport.start(URI.create("http://localhost"))
        verify(exactly = 1) { underlying.start(any()) }
        transport.stop()
        verify(exactly = 1) { underlying.stop() }
        transport.send("[[999,12345678]]")
        assert(msg.isCaptured)
        msg.clear()
        transport.send("[[888,12345678]]")
        assert(msg.isCaptured)
        assert(msg.captured == "[[888,12345678]]")
    }
}