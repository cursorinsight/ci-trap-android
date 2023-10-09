package com.cursorinsight.trap.util

import android.os.SystemClock
import android.util.Log
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@ExtendWith(MockKExtension::class)
class TrapLoggerTest {

    private lateinit var logger: TrapLogger

    @BeforeEach
    fun setUp() {
        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0

        mockkObject(TrapTime)
        every { TrapTime.getCurrentTime() } returns 0

        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0

        logger = TrapLogger()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @ParameterizedTest(name = "Messages sent every {0} ms should create {1} log events")
    @MethodSource("getData")
    fun `log messages`(interval: Long, numberOfMessages: Int) {
        for (i in 1..20) {
            every { TrapTime.getCurrentTime() } returns i * interval
            logger.logException("tag", "message", Exception())
        }

        verify(exactly = numberOfMessages) {
            Log.e("tag", "message", any())
        }
    }

    companion object {
        @JvmStatic
        fun getData(): List<Arguments> {
            return listOf<Arguments>(
                Arguments.of(6000L, 14),
                Arguments.of(7000L, 16),
                Arguments.of(12000L, 20)
            )
        }
    }
}