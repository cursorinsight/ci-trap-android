package com.cursorinsight.trap.transport

import android.icu.util.Output
import com.cursorinsight.trap.util.TrapFileCache
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL

@ExtendWith(MockKExtension::class)
class TrapHttpTransportTest {
    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test HTTP transport`() {
        var writeArray: CapturingSlot<ByteArray> = slot()

        mockkStatic(URI::class)
        every { URI.create(any()) } answers {
            val url = mockkClass(URI::class)
            every { url.scheme } returns "http"
            every { url.toURL() } answers {
                val target = mockkClass(URL::class)
                every { target.openConnection() } answers {
                    val http = mockkClass(HttpURLConnection::class)
                    every { http.setRequestProperty(any(), any()) } returns Unit
                    every { http.setRequestMethod(any()) } returns Unit
                    every { http.setDoOutput(any()) } returns Unit
                    every { http.setDoInput(any()) } returns Unit
                    every { http.setConnectTimeout(any()) } returns Unit
                    every { http.setReadTimeout(any()) } returns Unit
                    every { http.disconnect() } returns Unit
                    every { http.getOutputStream() } answers {
                        val stream = mockkClass(OutputStream::class)
                        every { stream.write(capture(writeArray), any(), any()) } returns Unit
                        every { stream.flush() } returns Unit
                        stream
                    }
                    every { http.getResponseCode() } returns 200


                    http
                }
                target
            }
            url
        }

        val transport = TrapHttpTransport(100, 100, compress = false)
        transport.start(URI.create("http://localhost"))
        transport.send("[[999]]")
        assert(writeArray.isCaptured)
        transport.stop()
    }
}