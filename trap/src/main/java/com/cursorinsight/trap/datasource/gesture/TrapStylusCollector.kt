package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.AXIS_TILT
import android.view.MotionEvent.TOOL_TYPE_ERASER
import android.view.MotionEvent.TOOL_TYPE_STYLUS
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Collects stylus events and packages them as
 * data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapStylusCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
): TrapDatasource {
    @OptIn(ExperimentalStdlibApi::class)
    private val handler = { event: MotionEvent? ->
        if (event != null && (event.getToolType(0) == TOOL_TYPE_STYLUS || event.getToolType(0) == TOOL_TYPE_ERASER)) {
            when (event.actionMasked) {
                ACTION_POINTER_DOWN, ACTION_DOWN -> {
                    val frame = JSONArray()
                    frame.put(StylusState.START.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.getAxisValue(AXIS_TILT))
                    frame.put(event.orientation)
                    storage.add(frame)
                }
                ACTION_MOVE -> {
                    for (pos in 0..<event.historySize) {
                        val frame = JSONArray()
                        frame.put(StylusState.MOVE.state)
                        frame.put(TrapTime.normalizeMillisecondTime(event.getHistoricalEventTime(pos)))
                        frame.put(event.getHistoricalX(pos))
                        frame.put(event.getHistoricalY(pos))
                        frame.put(event.getHistoricalPressure(pos))
                        frame.put(event.getHistoricalAxisValue(AXIS_TILT, pos))
                        frame.put(event.getHistoricalOrientation(pos))
                        storage.add(frame)
                    }
                }
                ACTION_POINTER_UP, ACTION_UP, ACTION_OUTSIDE, ACTION_CANCEL -> {
                    val frame = JSONArray()
                    frame.put(StylusState.END.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.getAxisValue(AXIS_TILT))
                    frame.put(event.orientation)
                    storage.add(frame)
                }
                else -> {
                    Log.w(TrapStylusCollector::class.simpleName, "Unknown stylus state ${event.actionMasked}")
                }
            }
        }
    }

    override fun start(activity: Activity) {
        TrapWindowCallback.addTouchHandler(handler)
    }

    override fun stop(activity: Activity) {
        TrapWindowCallback.removeTouchHandler(handler)
    }

    /**
     * The API identifiers for the state
     */
    private enum class StylusState(val state: Int) {
        START(110),
        MOVE(111),
        END(112)
    }
}