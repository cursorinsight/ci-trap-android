package com.cursorinsight.trap.datasource.gesture

import android.app.Activity
import android.view.MotionEvent
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapLogger
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Base class for processing MotionEvents.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
abstract class TrapMotionEventCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    config: TrapConfig,
): TrapDatasource {
    @OptIn(ExperimentalStdlibApi::class)
    private val logger = TrapLogger(config.maxNumberOfLogMessagesPerMinute)

    val handler = { event: MotionEvent? ->
        try {
            if (event != null) {
                val frames = mutableListOf<JSONArray>()

                processEvent(frames, event)

                if (frames.isNotEmpty()) {
                    TrapBackgroundExecutor.run {
                        for (frame in frames) {
                            storage.add(frame)
                        }
                    }
                }
            }
        }
        catch (ex: Exception) {
            logger.logException(
                TrapWindowCallback::class.simpleName,
                "Processing touch event failed",
                ex
            )
        }
    }

    abstract fun processEvent(frames: MutableList<JSONArray>, event: MotionEvent)

    override fun start(activity: Activity) {
        TrapWindowCallback.addTouchHandler(handler)
    }

    override fun stop(activity: Activity) {
        TrapWindowCallback.removeTouchHandler(handler)
    }

}