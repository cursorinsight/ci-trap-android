package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.os.SystemClock
import android.util.Log
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
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Suppress("SameParameterValue")
@ExtendWith(MockKExtension::class)
class StylusCollectorTest {
    private var initialWindowCallback: Window.Callback = run {
        val callback = mockk<Window.Callback>()
        every { callback.dispatchTouchEvent(any()) } returns true

        callback
    }

    private lateinit var windowCallback: Window.Callback

    @BeforeEach
    fun setUp() {
        windowCallback = TrapWindowCallback(initialWindowCallback)

        mockkStatic(SystemClock::class)
        every { SystemClock.elapsedRealtime() } returns 0
        every { SystemClock.uptimeMillis() } returns 0

        mockkStatic(Log::class)
        every { Log.v(any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.e(any(), any()) } returns 0
        every { Log.w(ofType(String::class), ofType(String::class)) } returns 0

        mockkObject(TrapBackgroundExecutor)
        every { TrapBackgroundExecutor.run(capture(command)) } returns Unit
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
        TrapWindowCallback.clear()
    }

    private fun getEvent(
        time: Long,
        action: Int,
        x: Float,
        y: Float,
        pressure: Float,
        axis: Float,
        orientation: Float
    ): MotionEvent {
        val event = mockkClass(MotionEvent::class)
        every { event.getToolType(0) } returns MotionEvent.TOOL_TYPE_STYLUS
        every { event.actionMasked } returns action
        every { event.eventTime } returns time
        every { event.rawX } returns x
        every { event.rawY } returns y
        every { event.pressure } returns pressure
        every { event.getHistoricalEventTime(any()) } returns time
        every { event.getHistoricalX(any()) } returns x
        every { event.getHistoricalX(any(), any()) } returns x
        every { event.getHistoricalY(any()) } returns y
        every { event.getHistoricalY(any(), any()) } returns y
        every { event.getAxisValue(MotionEvent.AXIS_TILT) } returns axis
        every { event.getX(any()) } returns x
        every { event.getY(any()) } returns y
        every { event.historySize } returns 2
        every { event.getHistoricalAxisValue(MotionEvent.AXIS_TILT, any()) } returns axis
        every { event.getHistoricalOrientation(any()) } returns orientation
        every { event.getHistoricalPressure(any()) } returns pressure
        every { event.getOrientation() } returns orientation

        return event
    }

    @ParameterizedTest
    @CsvSource(
        "true",
        "false"
    )
    fun `test stylus collection`(captureCoalescedEvents: Boolean) {
        val activity: Activity = mockk()
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val config = TrapConfig.DataCollection()
        config.collectCoalescedStylusEvents = captureCoalescedEvents
        val collector = TrapStylusCollector(storage)
        collector.start(activity, config)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_DOWN,
                15.0F,
                35.0F,
                1.0F,
                2.0F,
                3.0F
            )
        )

        assert(command.isCaptured)
        command.captured()
        Assertions.assertEquals(storage.size, 1)
        val el = storage.elementAt(0)
        Assertions.assertEquals(el.getInt(0), 110)
        Assertions.assertEquals(el.getLong(1), TrapTime.normalizeUptimeMillisecond(1L))
        assert(el.getDouble(2) == 15.0)
        assert(el.getDouble(3) == 35.0)
        Assertions.assertEquals(el.getDouble(4), 1.0)
        Assertions.assertEquals(el.getDouble(5), 2.0)
        Assertions.assertEquals(el.getDouble(6), 3.0)

        windowCallback.dispatchTouchEvent(
            getEvent(
                11L,
                MotionEvent.ACTION_MOVE,
                115.0F,
                135.0F,
                1.0F,
                2.0F,
                3.0F
            )
        )

        assert(command.isCaptured)
        command.captured()
        Assertions.assertEquals(storage.size, if (captureCoalescedEvents) 3 else 2)
        val el2 = storage.elementAt(1)
        Assertions.assertEquals(el2.getInt(0), 111)
        Assertions.assertEquals(el2.getLong(1), TrapTime.normalizeUptimeMillisecond(11L))
        Assertions.assertEquals(el2.getDouble(2), 115.0)
        Assertions.assertEquals(el2.getDouble(3), 135.0)
        Assertions.assertEquals(el2.getDouble(4), 1.0)
        Assertions.assertEquals(el2.getDouble(5), 2.0)
        Assertions.assertEquals(el2.getDouble(6), 3.0)

        windowCallback.dispatchTouchEvent(
            getEvent(
                111L,
                MotionEvent.ACTION_UP,
                1115.0F,
                1135.0F,
                1.0F,
                2.0F,
                3.0F
            )
        )

        assert(command.isCaptured)
        command.captured()
        Assertions.assertEquals(storage.size, if (captureCoalescedEvents) 4 else 3)
        val el3 = storage.elementAt(if (captureCoalescedEvents) 3 else 2)
        Assertions.assertEquals(el3.getInt(0), 112)
        Assertions.assertEquals(el3.getLong(1), TrapTime.normalizeUptimeMillisecond(111L))
        Assertions.assertEquals(el3.getDouble(2), 1115.0)
        Assertions.assertEquals(el3.getDouble(3), 1135.0)
        Assertions.assertEquals(el3.getDouble(4), 1.0)
        Assertions.assertEquals(el3.getDouble(5), 2.0)
        Assertions.assertEquals(el3.getDouble(6), 3.0)

        collector.stop(activity)
    }

    companion object {
        var command: CapturingSlot<() -> Unit> = slot()

    }
}