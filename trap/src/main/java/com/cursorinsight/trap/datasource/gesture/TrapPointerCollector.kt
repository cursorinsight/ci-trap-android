package com.cursorinsight.trap.datasource.gesture

import android.util.Log
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_MOVE
import android.view.MotionEvent.ACTION_OUTSIDE
import android.view.MotionEvent.ACTION_POINTER_DOWN
import android.view.MotionEvent.ACTION_POINTER_UP
import android.view.MotionEvent.ACTION_UP
import android.view.MotionEvent.BUTTON_PRIMARY
import android.view.MotionEvent.BUTTON_SECONDARY
import android.view.MotionEvent.BUTTON_TERTIARY
import android.view.MotionEvent.TOOL_TYPE_MOUSE
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Collects pointer events and packages them as
 * data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapPointerCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
): TrapMotionEventCollector(storage, config) {
    @OptIn(ExperimentalStdlibApi::class)
    override fun processEvent(frames: MutableList<JSONArray>, event: MotionEvent) {
        if (event.getToolType(0) == TOOL_TYPE_MOUSE) {
            when (event.actionMasked) {
                ACTION_POINTER_DOWN, ACTION_DOWN -> {
                    val frame = JSONArray()
                    frame.put(PointerState.START.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(
                        when {
                            event.isButtonPressed(BUTTON_PRIMARY) -> 0
                            event.isButtonPressed(BUTTON_SECONDARY) -> 1
                            event.isButtonPressed(BUTTON_TERTIARY) -> 2
                            else -> 999
                        }
                    )
                    frames.add(frame)
                }

                ACTION_MOVE -> {
                    for (pos in 0..<event.historySize) {
                        val frame = JSONArray()
                        frame.put(PointerState.MOVE.state)
                        frame.put(TrapTime.normalizeMillisecondTime(event.getHistoricalEventTime(pos)))
                        frame.put(event.getHistoricalX(pos))
                        frame.put(event.getHistoricalY(pos))
                        frame.put(
                            when {
                                event.isButtonPressed(BUTTON_PRIMARY) -> 0
                                event.isButtonPressed(BUTTON_SECONDARY) -> 1
                                event.isButtonPressed(BUTTON_TERTIARY) -> 2
                                else -> 999
                            }
                        )
                        frames.add(frame)
                    }
                }

                ACTION_POINTER_UP, ACTION_UP, ACTION_OUTSIDE, ACTION_CANCEL -> {
                    val frame = JSONArray()
                    frame.put(PointerState.END.state)
                    frame.put(TrapTime.normalizeMillisecondTime(event.eventTime))
                    frame.put(event.rawX)
                    frame.put(event.rawY)
                    frame.put(
                        when {
                            event.isButtonPressed(BUTTON_PRIMARY) -> 0
                            event.isButtonPressed(BUTTON_SECONDARY) -> 1
                            event.isButtonPressed(BUTTON_TERTIARY) -> 2
                            else -> 999
                        }
                    )
                    frames.add(frame)
                }

                else -> {
                    Log.w(
                        TrapPointerCollector::class.simpleName,
                        "Unknown pointer state ${event.actionMasked}"
                    )
                }
            }
        }
    }

    /**
     * The API identifiers for the state
     */
    private enum class PointerState(val state: Int) {
        START(0),
        MOVE(5),
        END(6)
    }
}