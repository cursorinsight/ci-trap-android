package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.BUTTON_TERTIARY
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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
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
class PointerCollectorTest {
    private var initialWindowCallback: Window.Callback = run {
        val callback = mockk<Window.Callback>()
        every { callback.dispatchTouchEvent(any()) } returns true

        callback
    }

    private lateinit var windowCallback: Window.Callback

    @BeforeEach
    fun setUp() {
        windowCallback = TrapWindowCallback(initialWindowCallback)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    private fun getEvent(
        time: Long,
        action: Int,
        x: Float,
        y: Float,
        button: Int
    ): MotionEvent {
        val pressed: CapturingSlot<Int> = slot()
        val event = mockkClass(MotionEvent::class)
        every { event.getToolType(0) } returns MotionEvent.TOOL_TYPE_MOUSE
        every { event.actionMasked } returns action
        every { event.eventTime } returns time
        every { event.rawX } returns x
        every { event.rawY } returns y
        every { event.isButtonPressed(capture(pressed)) } answers {
            button == pressed.captured
        }
        every { event.getHistoricalEventTime(any()) } returns time
        every { event.getHistoricalX(any()) } returns x
        every { event.getHistoricalX(any(), any()) } returns x
        every { event.getHistoricalY(any()) } returns y
        every { event.getHistoricalY(any(), any()) } returns y

        every { event.getX(any()) } returns x
        every { event.getY(any()) } returns y
        every { event.historySize } returns 1

        return event
    }

    @Test
    fun `test pointer data is collected`() {
        val activity: Activity = mockk()
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val collector = TrapPointerCollector(storage, TrapConfig())
        collector.start(activity)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_DOWN,
                15.0F,
                35.0F,
                BUTTON_TERTIARY,
            )
        )

        assert(command.isCaptured)
        command.captured()
        assertEquals(storage.size, 1)
        val el = storage.elementAt(0)
        assertEquals(el.getInt(0), 0)
        assertEquals(el.getLong(1), TrapTime.normalizeMillisecondTime(1L))
        assert(el.getDouble(2) == 15.0)
        assert(el.getDouble(3) == 35.0)
        assertEquals(el.getInt(4), 2)

        windowCallback.dispatchTouchEvent(
            getEvent(
                11L,
                MotionEvent.ACTION_MOVE,
                115.0F,
                135.0F,
                BUTTON_TERTIARY,
            )
        )

        assert(command.isCaptured)
        command.captured()
        assertEquals(storage.size, 2)
        val el2 = storage.elementAt(1)
        assertEquals(el2.getInt(0), 5)
        assertEquals(el2.getLong(1), TrapTime.normalizeMillisecondTime(11L))
        assertEquals(el2.getDouble(2), 115.0)
        assertEquals(el2.getDouble(3), 135.0)
        assertEquals(el2.getInt(4), 2)

        windowCallback.dispatchTouchEvent(
            getEvent(
                111L,
                MotionEvent.ACTION_UP,
                1115.0F,
                1135.0F,
                BUTTON_TERTIARY,
            )
        )

        assert(command.isCaptured)
        command.captured()
        assertEquals(storage.size, 3)
        val el3 = storage.elementAt(2)
        assertEquals(el3.getInt(0), 6)
        assertEquals(el3.getLong(1), TrapTime.normalizeMillisecondTime(111L))
        assertEquals(el3.getDouble(2), 1115.0)
        assertEquals(el3.getDouble(3), 1135.0)
        assertEquals(el3.getInt(4), 2)

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