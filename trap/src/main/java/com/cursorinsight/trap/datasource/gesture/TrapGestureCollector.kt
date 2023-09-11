package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.view.GestureDetector
import android.view.InputDevice
import android.view.MotionEvent
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Collects basic gesture events and packages them as
 * data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
@Suppress("unused")
class TrapGestureCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
) : TrapDatasource, GestureDetector.OnGestureListener {
    private val tapEventType = 122

    private lateinit var gestureHandler: GestureDetector

    private val handler = { event: MotionEvent? ->
        if (event != null
            && (event.source == InputDevice.SOURCE_TOUCHSCREEN || event.source == InputDevice.SOURCE_TOUCHPAD)
        ) {
            gestureHandler.onTouchEvent(event)
        }
    }

    override fun start(activity: Activity) {
        gestureHandler = GestureDetector(activity, this)
        TrapWindowCallback.addTouchHandler(handler)
    }

    override fun stop(activity: Activity) {
        TrapWindowCallback.removeTouchHandler(handler)
    }

    override fun onDown(p0: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(p0: MotionEvent) { }

    override fun onSingleTapUp(p0: MotionEvent): Boolean {
        with(JSONArray()) {
            put(tapEventType)
            put(TrapTime.normalizeMillisecondTime(p0.eventTime))
            put(p0.rawX)
            put(p0.rawY)
        }.let {
            if (it.length() > 0) {
                storage.add(it)
            }
        }

        return false
    }

    override fun onScroll(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

    override fun onLongPress(p0: MotionEvent) { }

    override fun onFling(p0: MotionEvent, p1: MotionEvent, p2: Float, p3: Float): Boolean {
        return false
    }

}