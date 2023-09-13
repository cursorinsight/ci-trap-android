package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.util.Log
import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import android.view.Window
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapTime
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
import io.mockk.verify
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Suppress("SameParameterValue")
@ExtendWith(MockKExtension::class)
class GestureCollectorTest {
    private var initialWindowCallback: Window.Callback = run {
        val callback = mockk<Window.Callback>()
        every { callback.dispatchTouchEvent(any()) } returns true

        callback
    }

    private lateinit var windowCallback: Window.Callback

    @BeforeEach
    fun setUp() {
        mockkConstructor(GestureDetector::class)
        every { anyConstructed<GestureDetector>().onTouchEvent(any()) } returns true
        windowCallback = TrapWindowCallback(initialWindowCallback)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        TrapWindowCallback.clear()
    }

    @Test
    fun `test gesture collection`() {
        val activity: Activity = mockk()
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapGestureCollector(storage, TrapConfig())
        val event = run {
            val event = mockkClass(MotionEvent::class)
            every { event.source } returns InputDevice.SOURCE_TOUCHSCREEN
            every { event.eventTime } returns 111L
            every { event.rawX } returns 55.0F
            every { event.rawY } returns 135.0F
            event
        }

        collector.start(activity)

        windowCallback.dispatchTouchEvent(event)

        verify(exactly = 1) { anyConstructed<GestureDetector>().onTouchEvent(event) }

        assertEquals(collector.onSingleTapUp(event), false)

        assert(command.isCaptured)
        command.captured()

        assertEquals(storage.size, 1)
        val el = storage.elementAt(0)
        assertEquals(el.getInt(0), 122)
        assertEquals(el.getLong(1), TrapTime.normalizeUptimeMillisecond(111L))
        assertEquals(el.getDouble(2), 55.0)
        assertEquals(el.getDouble(3), 135.0)

        assertEquals(collector.onDown(event), false)
        assertEquals(collector.onShowPress(event), Unit)
        assertEquals(collector.onScroll(event, event, 1.0F, 1.0F), false)
        assertEquals(collector.onLongPress(event), Unit)
        assertEquals(collector.onFling(event, event, 1.0F, 1.0F), false)

        collector.stop(activity)
    }

    companion object {
        var command: CapturingSlot<() -> Unit> = slot()

        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
            every { Log.w(ofType(String::class), ofType(String::class)) } returns 0

            mockkObject(TrapBackgroundExecutor)
            every { TrapBackgroundExecutor.run(capture(command)) } returns Unit
        }
    }
}