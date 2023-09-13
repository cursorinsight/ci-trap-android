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
import io.mockk.impl.annotations.MockK
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
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

@Suppress("SameParameterValue")
@ExtendWith(MockKExtension::class)
class TouchCollectorTest {
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
        touchMajor: Float,
        fingerId: Int
    ): MotionEvent {
        val event = mockkClass(MotionEvent::class)
        every { event.getToolType(0) } returns MotionEvent.TOOL_TYPE_FINGER
        every { event.actionMasked } returns action
        every { event.getPointerId(any()) } returns fingerId
        every { event.actionIndex } returns 0
        every { event.eventTime } returns time
        every { event.getHistoricalEventTime(any()) } returns time
        every { event.rawX } returns x
        every { event.rawY } returns y
        every { event.pressure } returns pressure
        every { event.touchMajor } returns touchMajor
        every { event.findPointerIndex(any()) } returns 0
        every { event.getX(any()) } returns x
        every { event.getY(any()) } returns y
        every { event.getPressure(any()) } returns pressure
        every { event.getTouchMajor(any()) } returns touchMajor
        every { event.pointerCount } returns 1
        every { event.historySize } returns 2
        every { event.getHistoricalX(any(), any()) } returns x
        every { event.getHistoricalY(any(), any()) } returns y
        every { event.getHistoricalPressure(any(), any()) } returns pressure
        every { event.getHistoricalTouchMajor(any(), any()) } returns touchMajor

        return event
    }

    @ParameterizedTest
    @CsvSource(
        "true",
        "false"
    )
    fun `test touch data is collected`(captureCoalescedEvents: Boolean, @MockK activity: Activity) {
        val storage = SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(100))
        val config = TrapConfig()
        config.collectCoalescedTouchEvents = captureCoalescedEvents
        val collector = TrapTouchCollector(storage, config)
        collector.start(activity)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_DOWN,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured)
        command.captured()
        command.clear()

        assert(storage.size == 1)
        val el = storage.elementAt(0)
        assert(el.getInt(0) == 100)
        assert(el.getLong(1) == TrapTime.normalizeUptimeMillisecond(1L))
        assert(el.getInt(2) == 0)
        assert(el.getDouble(3) == 15.0)
        assert(el.getDouble(4) == 35.0)
        assert(el.getDouble(5) == 66.0)
        assert(el.getDouble(6) == 33.0)


        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_POINTER_DOWN,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured)
        command.captured()
        command.clear()

        assert(storage.size == 2)
        val el2 = storage.elementAt(1)
        assert(el2.getInt(0) == 100)
        assert(el2.getLong(1) == TrapTime.normalizeUptimeMillisecond(1L))
        assert(el2.getInt(2) == 0)
        assert(el2.getDouble(3) == 15.0)
        assert(el2.getDouble(4) == 35.0)
        assert(el2.getDouble(5) == 66.0)
        assert(el2.getDouble(6) == 33.0)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_MOVE,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured)
        command.captured()
        command.clear()

        assert(storage.size == if (captureCoalescedEvents) 4 else 3)
        val el3 = storage.elementAt(2)
        assert(el3.getInt(0) == 101)
        assert(el3.getLong(1) == TrapTime.normalizeUptimeMillisecond(1L))
        assert(el3.getInt(2) == 0)
        assert(el3.getDouble(3) == 15.0)
        assert(el3.getDouble(4) == 35.0)
        assert(el3.getDouble(5) == 66.0)
        assert(el3.getDouble(6) == 33.0)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_UP,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured)
        command.captured()
        command.clear()

        assert(storage.size == if (captureCoalescedEvents) 5 else 4)
        val el4 = storage.elementAt(if (captureCoalescedEvents) 4 else 3)
        assert(el4.getInt(0) == 102)
        assert(el4.getLong(1) == TrapTime.normalizeUptimeMillisecond(1L))
        assert(el4.getInt(2) == 0)
        assert(el4.getDouble(3) == 15.0)
        assert(el4.getDouble(4) == 35.0)
        assert(el4.getDouble(5) == 66.0)
        assert(el4.getDouble(6) == 33.0)


        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_POINTER_UP,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured)
        command.captured()
        command.clear()

        assert(storage.size == if (captureCoalescedEvents) 6 else 5)
        val el5 = storage.elementAt(if (captureCoalescedEvents) 5 else 4)
        assert(el5.getInt(0) == 102)
        assert(el5.getLong(1) == TrapTime.normalizeUptimeMillisecond(1L))
        assert(el5.getInt(2) == 0)
        assert(el5.getDouble(3) == 15.0)
        assert(el5.getDouble(4) == 35.0)
        assert(el5.getDouble(5) == 66.0)
        assert(el5.getDouble(6) == 33.0)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                MotionEvent.ACTION_CANCEL,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured == false)
        assert(storage.size == if (captureCoalescedEvents) 6 else 5)

        windowCallback.dispatchTouchEvent(
            getEvent(
                1L,
                9999,
                15.0F,
                35.0F,
                66F,
                33F,
                0
            )
        )

        assert(command.isCaptured == false)
        assert(storage.size == if (captureCoalescedEvents) 6 else 5)

        collector.stop(activity)
    }

    companion object {
        var command: CapturingSlot<() -> Unit> = slot()
    }
}