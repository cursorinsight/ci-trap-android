package com.cursorinsight.trap.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.WIFI_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapPermissionActivity
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Monitors for Wifi networks and connections then reports them
 * as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapWiFiCollector(
    private val storage: SynchronizedQueue<JSONArray>
): TrapDatasource {
    /**
     * The Trap event type for wifi
     */
    private val wifiEventType = 107

    /**
     * The system service for wifi.
     */
    private var wifiManager: WifiManager? = null

    /**
     * The system service for any network connectivity.
     */
    private var connectivityManager: ConnectivityManager? = null

    /**
     * The callback for the connectivity service.
     */
    private var connectivityReceiver = object : ConnectivityManager.NetworkCallback() {
        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            if (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                processScan()
            }
        }
    }

    /**
     * If true, then we have a registered location handler we need to
     * deregister.
     */
    private var registered = false

    /**
     * The callback for the wifi service.
     */
    private val wifiReceiver = object : BroadcastReceiver() {
        @Suppress("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            try {
                processScan()
            } catch (ex: Exception) {
                Log.e(
                    TrapWiFiCollector::class.simpleName,
                    "Processing WIFI scan result failed",
                    ex
                )
            }
        }
    }

    /**
     * Process the scan results
     */
    @SuppressLint("MissingPermission")
    private fun processScan() {
        val results = wifiManager?.scanResults
        with(JSONArray()) {
            put(wifiEventType)
            put(TrapTime.getCurrentTime())
            put(with(JSONArray()) {
                results?.forEach {result ->
                    put(with(JSONArray()) {
                        val ssid = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            result.wifiSsid?.let { String(it.bytes) }
                        } else {
                            @Suppress("DEPRECATION")  result.SSID
                        }
                        put(ssid)
                        put(result.BSSID)
                        this
                    })
                }
                this
            })
            this
        }.let {
            if ((results?.size ?: 0) > 0) {
                storage.add(it)
            }
        }
    }

    override fun start(activity: Activity, config: TrapConfig.DataCollection) {
        wifiManager = activity.getSystemService(WIFI_SERVICE) as WifiManager
        connectivityManager = activity.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        processScan()

        activity.registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        connectivityManager?.registerDefaultNetworkCallback(connectivityReceiver)
        registered = true

        @Suppress("DEPRECATION")
        val success = wifiManager?.startScan() ?: false
        if (!success) {
            Log.i("NetworkManager", "WiFi scan failure")
        }
    }

    override fun stop(activity: Activity) {
        if (registered) {
            activity.unregisterReceiver(wifiReceiver)
            connectivityManager?.unregisterNetworkCallback(connectivityReceiver)
        }
    }

    companion object {
        /**
         * Check if the application has Bluetooth collection permissions
         *
         * @param activity The android Activity instance.
         * @return Returns true if the permissions this collector needs have been set.
         */
        fun checkPermissions(activity: Activity): Boolean {
            val networkStateOk = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_NETWORK_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val wifiState = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val changeStateOk = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.CHANGE_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED

            val locationOk = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return networkStateOk && wifiState && changeStateOk && locationOk
        }

        /**
         * Ask the system for the required permissions.
         *
         * @param activity The android Activity instance.
         * @param onSuccess Calls this closure if the user gives permission.
         */
        @SuppressLint("UnspecifiedRegisterReceiverFlag")
        fun requirePermissions(activity: Activity, onSuccess: () -> Unit) {
            if (!checkPermissions(activity)) {
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(p0: Context?, p1: Intent?) {
                        onSuccess()
                    }
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    activity.registerReceiver(
                        receiver,
                        IntentFilter("com.cursorinsight.trap.WIFI_PERMISSION"),
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    activity.registerReceiver(
                        receiver,
                        IntentFilter("com.cursorinsight.trap.WIFI_PERMISSION")
                    )
                }
                val permissionActivityIntent = Intent(activity, TrapPermissionActivity::class.java)
                permissionActivityIntent.putExtra(
                    "PERMISSION",
                    TrapPermissionActivity.Permissions.WIFI.code
                )
                activity.startActivity(permissionActivityIntent)
            } else {
                onSuccess()
            }
        }
    }
}