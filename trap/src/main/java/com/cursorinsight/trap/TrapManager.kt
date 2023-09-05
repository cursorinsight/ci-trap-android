package com.cursorinsight.trap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.transport.TrapReporter
import org.apache.commons.collections4.queue.CircularFifoQueue
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import java.lang.ref.WeakReference

/**
 * The central manager and orchestrator of the collection
 * platform system.
 *
 * @constructor
 * Launches all default collectors and initializes the reporter subtask.
 *
 * @param application The android Application instance.
 * @param config The library config.
 */
class TrapManager internal constructor(
    application: Application,
    private var config: TrapConfig,
) : Application.ActivityLifecycleCallbacks {
    /**
     * Represents the common storage for all collectors
     * and the reporter task.
     */
    private val buffer =
        SynchronizedQueue.synchronizedQueue(CircularFifoQueue<JSONArray>(config.queueSize))

    /**
     * The reporter system.
     */
    private val reporter = TrapReporter(config, application, buffer)

    /**
     * The registered and running collector
     * instances.
     */
    private var collectors: MutableMap<Int, TrapDatasource>

    /**
     * The cached current activity the run and halt methods
     * can use to mange individual collectors.
     */
    private var currentActivity: WeakReference<Activity>? = null

    companion object {
        private var instance: TrapManager? = null

        /**
         * Get the singleton instance of the manager.
         */
        fun getInstance(application: Application, configuration: TrapConfig? = null): TrapManager {
            if (configuration != null && instance?.config != configuration) {
                instance = TrapManager(application, configuration)
            }

            return instance ?: TrapManager(application, TrapConfig()).also { instance = it }
        }
    }

    init {
        // Register the activity collection and lifecycle dispatch.
        application.registerActivityLifecycleCallbacks(this)

        // Init the collector collection
        collectors = mutableMapOf()
        for (collector in config.collectors) {
            val instance = try {
                val constructor = collector.java.getConstructor(
                    SynchronizedQueue::class.java,
                    TrapConfig::class.java
                )
                constructor.newInstance(buffer, config) as? TrapDatasource
            } catch (_: Exception) {
                Log.e(
                    TrapManager::class.simpleName,
                    "The provided class '${collector.simpleName}' doesn't have a proper constructor signature"
                )
                null
            }

            if (instance != null) {
                collectors[instance.hashCode()] = instance
            }
        }
    }

    /**
     * Adds a collector instance to the platform and
     * starts it immediately.
     */
    @Suppress("unused")
    fun run(collector: TrapDatasource) {
        if (!collectors.containsKey(collector.hashCode())) {
            reporter.start()
            val activity = currentActivity?.get()
            if (activity != null) {
                collector.start(activity)
            }
            collectors[collector.hashCode()] = collector
        }
    }

    /**
     * Stops and removes a collector from the platform.
     */
    @Suppress("unused")
    fun halt(collector: TrapDatasource) {
        val activity = currentActivity?.get()
        if (activity != null) {
            collectors[collector.hashCode()]?.stop(activity)
        }
        reporter.stop()
    }

    /**
     * Run all default collectors set in the configuration.
     */
    private fun runAll(activity: Activity) {
        reporter.start()
        for (collector in collectors.values) {
            collector.start(activity)
        }
    }

    /**
     * Stop all collectors currently running.
     */
    private fun haltAll(activity: Activity) {
        for (collector in collectors.values) {
            collector.stop(activity)
        }
        reporter.stop()
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        activity.window.callback = TrapWindowCallback(activity.window.callback)
    }

    override fun onActivityResumed(activity: Activity) {
        currentActivity = WeakReference(activity)
        runAll(activity)
    }

    override fun onActivityPaused(activity: Activity) {
        haltAll(activity)
        currentActivity = WeakReference(null)
    }

    override fun onActivityStopped(activity: Activity) {}

    override fun onActivityStarted(activity: Activity) {}

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}

    override fun onActivityDestroyed(activity: Activity) {}
}
