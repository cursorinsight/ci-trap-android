package com.cursorinsight.trap

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.content.getSystemService
import com.cursorinsight.trap.datasource.TrapDatasource
import com.cursorinsight.trap.datasource.TrapMetadataCollector
import com.cursorinsight.trap.datasource.gesture.internal.TrapWindowCallback
import com.cursorinsight.trap.transport.TrapReporter
import com.cursorinsight.trap.util.TrapTime
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
    private var collectors: MutableMap<String, TrapDatasource>

    /**
     * The cached current activity the run and halt methods
     * can use to mange individual collectors.
     */
    private var currentActivity: WeakReference<Activity>? = null

    private var hasLowBattery: Boolean = false

    private var inLowDataMode: Boolean = false

    private var currentDataCollectionConfig: TrapConfig.DataCollection = config.defaultDataCollection

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val batteryManager = context.getSystemService<BatteryManager>()

                val batteryIntent = context.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )

                hasLowBattery = !(batteryManager?.isCharging ?: false) &&
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        batteryIntent?.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
                            ?: false
                    }
                    else {
                        (batteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 100) < 10
                    }

                maybeModifyConfigAndRestartCollection()
            } catch (ex: Exception) {
                Log.e(
                    TrapManager::class.simpleName,
                    "Processing battery change failed",
                    ex
                )
            }
        }
    }

    private val networkReceiver = object: ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            try {
                super.onCapabilitiesChanged(network, networkCapabilities)
                inLowDataMode = !networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
                maybeModifyConfigAndRestartCollection()
            } catch (ex: Exception) {
                Log.e(
                    TrapManager::class.simpleName,
                    "Processing network change failed",
                    ex
                )
            }
        }
    }

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

        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()

        connectivityManager.registerNetworkCallback(networkRequest, networkReceiver)

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_OKAY)
        filter.addAction(Intent.ACTION_BATTERY_LOW)
        filter.addAction(Intent.ACTION_POWER_CONNECTED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        application.registerReceiver(batteryReceiver, filter)

        // Init the collector collection
        collectors = mutableMapOf()
        for (collector in currentDataCollectionConfig.collectors) {
            createCollector(collector)
        }
    }

    /**
     * Adds a collector instance to the platform and
     * starts it immediately.
     */
    @Suppress("unused")
    fun run(collector: TrapDatasource) {
        try {
            if (!collectors.containsKey(collector.getName())) {
                reporter.start()
                val activity = currentActivity?.get()
                if (activity != null) {
                    collector.start(activity, currentDataCollectionConfig)
                }
                collectors[collector.getName()] = collector
            }
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Starting collector and reporter in run() failed",
                ex
            )
        }
    }

    /**
     * Stops and removes a collector from the platform.
     */
    @Suppress("unused")
    fun halt(collector: TrapDatasource) {
        try {
            val activity = currentActivity?.get()
            if (activity != null) {
                collectors[collector.getName()]?.stop(activity)
            }
            reporter.stop()
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Stopping collector and reporter in halt() failed",
                ex
            )
        }
    }

    /**
     * Adds a custom key-value to the metadata event.
     */
    @Suppress("unused")
    fun addCustomMetadata(key: String, value: String) {
        val metaDataCollector = collectors[TrapMetadataCollector::class.qualifiedName] as TrapMetadataCollector?
        metaDataCollector?.addCustom(key, value)
    }

    /**
     * Removes a custom key-value from the metadata event.
     */
    @Suppress("unused")
    fun removeCustomMetadata(key: String) {
        val metaDataCollector = collectors[TrapMetadataCollector::class.qualifiedName] as TrapMetadataCollector?
        metaDataCollector?.removeCustom(key)
    }

    /**
     * Adds a custom event to the event stream.
     */
    @Suppress("unused")
    fun addCustomEvent(custom: Any) {
        val customEventType = 132
        buffer.add(with(JSONArray()) {
            put(customEventType)
            put(TrapTime.getCurrentTime())
            put(custom)
            this
        })
    }

    /**
     * Run all default collectors set in the configuration.
     */
    private fun runAll(activity: Activity) {
        try {
            reporter.start()
            buffer.add(startMessage())
            currentDataCollectionConfig = getDataCollectionConfig()
            for (collectorQualifiedName in currentDataCollectionConfig.collectors) {
                if (!collectors.containsKey(collectorQualifiedName)) {
                    createCollector(collectorQualifiedName)
                }
                collectors[collectorQualifiedName]?.start(activity, currentDataCollectionConfig)
            }
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Starting reporter and collectors in runAll() failed",
                ex
            )
        }
    }

    /**
     * Returns the data collection config that should be used
     */
    private fun getDataCollectionConfig(): TrapConfig.DataCollection {
        if (inLowDataMode) {
            return config.lowDataDataCollection
        }
        if (hasLowBattery) {
            return config.lowBatteryDataCollection
        }
        return config.defaultDataCollection
    }

    /**
     * Stop all collectors currently running.
     */
    private fun haltAll(activity: Activity) {
        try {
            for (collector in collectors.values) {
                collector.stop(activity)
            }
            buffer.add(stopMessage())
            reporter.stop()
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Stopping collectors and reporter in haltAll() failed",
                ex
            )
        }
    }

    /**
     * Generates the start frame
     *
     * @return The start data frame.
     */
    private fun startMessage(): JSONArray {
        val startEventType = 130
        return with(JSONArray()) {
            put(startEventType)
            put(TrapTime.getCurrentTime())
            put(inLowDataMode)
            put(hasLowBattery)
            this
        }
    }

    /**
     * Generates the stop frame
     *
     * @return The stop data frame.
     */
    private fun stopMessage(): JSONArray {
        val stopEventType = 131
        return with(JSONArray()) {
            put(stopEventType)
            put(TrapTime.getCurrentTime())
            this
        }
    }

    /**
     * Restarts the collection if the config has to be changed
     */
    private fun maybeModifyConfigAndRestartCollection() {
        val activity = currentActivity?.get()
        if (activity != null) {
            if (getDataCollectionConfig() != currentDataCollectionConfig) {
                haltAll(activity)
                runAll(activity)
            }
        }
    }

    /*
     * Initializes a collector
     */
    private fun createCollector(collectorQualifiedName: String) {
        val instance = try {
            val constructor = Class.forName(collectorQualifiedName).getConstructor(
                SynchronizedQueue::class.java
            )
            constructor.newInstance(buffer) as? TrapDatasource
        } catch (_: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "The provided class '${collectorQualifiedName}' doesn't have a proper constructor signature"
            )
            null
        }

        if (instance != null) {
            collectors[collectorQualifiedName] = instance
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        try {
            activity.window.callback = TrapWindowCallback(activity.window.callback)
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Setting window callback failed",
                ex
            )
        }
    }

    override fun onActivityResumed(activity: Activity) {
        TrapTime.updateTimeDiff()
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
