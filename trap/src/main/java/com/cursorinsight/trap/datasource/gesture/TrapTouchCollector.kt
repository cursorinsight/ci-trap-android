package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.TOOL_TYPE_UNKNOWN
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Collects raw touch events and packages them as
 * data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapTouchCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
) : TrapDatasource {
    private val handler = { event: MotionEvent? ->
        if (event != null && (event.getToolType(0) == MotionEvent.TOOL_TYPE_FINGER || event.getToolType(0) == TOOL_TYPE_UNKNOWN)) {
            when(event.actionMasked) {
                ACTION_DOWN -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.START.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    storage.add(frame)
                }

                ACTION_POINTER_DOWN -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.START.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.getX(event.findPointerIndex(fingerId)))
                    frame.put(event.getY(event.findPointerIndex(fingerId)))
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    storage.add(frame)
                }

                ACTION_MOVE -> {
                    for (idx in 0..<event.pointerCount) {
                        val fingerId = event.getPointerId(idx)
                        for (pos in 0..<event.historySize) {
                            val frame = JSONArray()
                            frame.put(TouchState.MOVE.state)
                            frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                            frame.put(fingerId)
                            frame.put(event.getHistoricalX(idx, pos))
                            frame.put(event.getHistoricalY(idx, pos))
                            frame.put(event.getHistoricalPressure(idx, pos))
                            frame.put(event.getHistoricalTouchMajor(idx, pos))
                            storage.add(frame)
                        }
                    }
                }

                ACTION_UP -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.END.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    storage.add(frame)
                }

                ACTION_POINTER_UP -> {
                    val fingerId = event.getPointerId(event.actionIndex)
                    val frame = JSONArray()
                    frame.put(TouchState.END.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(fingerId)
                    frame.put(event.getX(event.findPointerIndex(fingerId)))
                    frame.put(event.getY(event.findPointerIndex(fingerId)))
                    frame.put(event.pressure)
                    frame.put(event.touchMajor)
                    storage.add(frame)
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

    override fun start(activity: Activity) {
        TrapWindowCallback.addTouchHandler(handler)
    }

    override fun stop(activity: Activity) {
        TrapWindowCallback.removeTouchHandler(handler)
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