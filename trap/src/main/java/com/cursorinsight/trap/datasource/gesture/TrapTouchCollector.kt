package com.cursorinsight.trap.datasource.gesture

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import com.cursorinsight.trap.util.TrapTime
import org.json.JSONArray

/**
 * Collects raw touch events and packages them as
 * data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapTouchCollector(): TrapMotionEventCollector() {
    @OptIn(ExperimentalStdlibApi::class)
    override fun processEvent(frames: MutableList<JSONArray>, event: MotionEvent) {
        if (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER || event.getToolType(0) == TOOL_TYPE_UNKNOWN) {
            when(event.actionMasked) {
                ACTION_DOWN -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.START.state)
                    frame.put(TrapTime.normalizeUptimeMillisecond(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    frames.add(frame)
                }

                ACTION_POINTER_DOWN -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.START.state)
                    frame.put(TrapTime.normalizeUptimeMillisecond(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.getX(event.findPointerIndex(fingerId)))
                    frame.put(event.getY(event.findPointerIndex(fingerId)))
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    frames.add(frame)
                }

                ACTION_MOVE -> {
                    for (idx in 0..<event.pointerCount) {
                        val fingerId = event.getPointerId(idx)
                        if (config?.collectCoalescedTouchEvents == true && event.historySize > 0) {
                            for (pos in 0..<event.historySize) {
                                val frame = JSONArray()
                                frame.put(TouchState.MOVE.state)
                                frame.put(TrapTime.normalizeUptimeMillisecond(
                                    event.getHistoricalEventTime(pos))
                                )
                                frame.put(fingerId)
                                frame.put(event.getHistoricalX(idx, pos))
                                frame.put(event.getHistoricalY(idx, pos))
                                frame.put(event.getHistoricalPressure(idx, pos))
                                frame.put(event.getHistoricalTouchMajor(idx, pos))
                                frames.add(frame)
                            }
                        } else {
                            val frame = JSONArray()
                            frame.put(TouchState.MOVE.state)
                            frame.put(TrapTime.normalizeUptimeMillisecond(event.eventTime))
                            frame.put(fingerId)
                            frame.put(event.getX(idx))
                            frame.put(event.getY(idx))
                            frame.put(event.getPressure(idx))
                            frame.put(event.getTouchMajor(idx))
                            frames.add(frame)
                        }
                    }
                }

                ACTION_UP -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.END.state)
                    frame.put(TrapTime.normalizeUptimeMillisecond(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    frames.add(frame)
                }

                ACTION_POINTER_UP -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.END.state)
                    frame.put(TrapTime.normalizeUptimeMillisecond(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.getX(event.findPointerIndex(fingerId)))
                    frame.put(event.getY(event.findPointerIndex(fingerId)))
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    frames.add(frame)
                }

                ACTION_CANCEL -> {
                    Log.e(
                        TrapTouchCollector::class.simpleName,
                        "Touch cancelled!"
                    )
                }

                else -> {
                    Log.w(
                        TrapTouchCollector::class.simpleName,
                        "Unknown touch state ${event.actionMasked}"
                    )
                }
            }
        }
    }

    /**
     * The API identifiers for the state
     */
    private enum class TouchState(val state: Int) {
        START(100),
        MOVE(101),
        END(102)
    }
}