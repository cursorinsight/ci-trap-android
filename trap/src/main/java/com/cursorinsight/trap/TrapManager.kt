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
import org.json.JSONObject
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
    private val application: Application,
    private var config: TrapConfig,
    private var currentActivity: WeakReference<Activity?>
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

    private var hasLowBattery: Boolean = false

    private var inLowDataMode: Boolean? = null

    private var isRunning: Boolean = false

    private var isEnabled: Boolean = true

    private lateinit var currentDataCollectionConfig: TrapConfig.DataCollection

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            try {
                val newBatteryStatus = getLowBatteryStatus()
                if (newBatteryStatus != hasLowBattery) {
                    hasLowBattery = newBatteryStatus
                    maybeModifyConfigAndRestartCollection()
                }
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
                val newLowDataMode = !networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
                if (newLowDataMode != inLowDataMode) {
                    inLowDataMode = newLowDataMode
                    maybeModifyConfigAndRestartCollection()
                }
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
        fun getInstance(
            application: Application,
            configuration: TrapConfig? = null,
            activity: Activity? = null): TrapManager {
            if (configuration != null && instance?.config != configuration) {
                instance = TrapManager(application, configuration, WeakReference(activity))
            }

            return instance ?: TrapManager(
                application, TrapConfig(), WeakReference(activity)).also { instance = it }
        }
    }

    init {
        isEnabled = config.enableDataCollection
        // Register the activity collection and lifecycle dispatch.
        application.registerActivityLifecycleCallbacks(this)
        val activity = currentActivity?.get()
        if (activity != null) {
            setWindowCallback(activity)
        }
        currentDataCollectionConfig = config.defaultDataCollection

        // Init the collector collection
        collectors = mutableMapOf()
        for (collector in currentDataCollectionConfig.collectors) {
             collectors[collector.getName()] = collector
        }
    }

    /**
     * Adds a collector instance to the platform and
     * starts it immediately.
     */
    @Suppress("unused")
    fun run(collector: TrapDatasource) {
        try {
            if (config.isDataCollectionDisabled()) {
                Log.i(TrapManager::class.simpleName, "Data collection disabled")
                return;
            }

            if (!collectors.containsKey(collector.getName())) {
                reporter.start(inLowDataMode ?: false)
                val activity = currentActivity?.get()
                if (activity != null) {
                    collector.start(activity, currentDataCollectionConfig, buffer)
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
    fun addCustomMetadata(key: String, value: Any) {
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
            if (config.isDataCollectionDisabled()) {
                Log.i(TrapManager::class.simpleName, "Data collection disabled")
                return;
            }
            hasLowBattery = getLowBatteryStatus()
            inLowDataMode = null
            currentDataCollectionConfig = getDataCollectionConfig()
            subscribeOnNotifications()

            startReporterAndCollectors(activity)
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Starting reporter and collectors in runAll() failed",
                ex
            )
        }
    }

    private fun startReporterAndCollectors(activity: Activity) {
        val actualDataMode = inLowDataMode
        if (actualDataMode != null) {
            isRunning = true
            reporter.start(actualDataMode)
            buffer.add(startMessage())
            for (collector in currentDataCollectionConfig.collectors) {
                val collectorKey = collector.getName()
                if (!collectors.containsKey(collectorKey)) {
                    collectors[collectorKey] = collector
                }
                collectors[collectorKey]?.start(
                    activity,
                    currentDataCollectionConfig,
                    buffer)
            }
        }
    }

    private fun getLowBatteryStatus() : Boolean {
        val batteryIntent = application.registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )

        val status: Int = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging: Boolean = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL

        val level: Int = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale: Int = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercentage : Float = level * 100 / scale.toFloat()

        return isCharging &&
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    batteryIntent?.getBooleanExtra(BatteryManager.EXTRA_BATTERY_LOW, false)
                        ?: false
                }
                else {
                    batteryPercentage <= config.lowBatteryThreshold
                }
    }

    /**
     * Returns the data collection config that should be used
     */
    private fun getDataCollectionConfig(): TrapConfig.DataCollection {
        if (inLowDataMode == true) {
            return config.lowDataDataCollection
        }
        if (hasLowBattery == true) {
            return config.lowBatteryDataCollection
        }
        return config.defaultDataCollection
    }

    /**
     * Stop all collectors currently running.
     */
    private fun haltAll(activity: Activity) {
        try {
            stopReporterAndCollectors(activity)
            unsubscribeFromNotifications()
        } catch (ex: Exception) {
            Log.e(
                TrapManager::class.simpleName,
                "Stopping collectors and reporter in haltAll() failed",
                ex
            )
        }
    }

    private fun stopReporterAndCollectors(activity: Activity) {
        if (isRunning) {
            isRunning = false
            for (collector in collectors.values) {
                collector.stop(activity)
            }
            buffer.add(stopMessage())
            reporter.stop()
        }
    }

    /**
     * Stop and disable the data collection
     */
    @Suppress("unused")
    fun disableDataCollection() {
        isEnabled = false
        val activity = currentActivity?.get()
        if (activity != null) {
            haltAll(activity)
        }
    }

    /**
     * Restart the data collection
     */
    @Suppress("unused")
    fun enableDataCollection() {
        isEnabled = true
        val activity = currentActivity?.get()
        if (activity != null) {
            runAll(activity)
        }
    }

    /**
     * Subscribe on battery and network notifications
     */
    private fun subscribeOnNotifications() {
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
    }

    /*
     * Unsubscribe from battery and network notifications
     **/
    private fun unsubscribeFromNotifications() {
        val connectivityManager = application.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connectivityManager.unregisterNetworkCallback(networkReceiver)
        application.unregisterReceiver(batteryReceiver)
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
            stopReporterAndCollectors(activity)
            currentDataCollectionConfig = getDataCollectionConfig()
            startReporterAndCollectors(activity)
        }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        setWindowCallback(activity)
    }

    private fun setWindowCallback(activity: Activity) {
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
        if (isEnabled) {
            runAll(activity)
        }
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
