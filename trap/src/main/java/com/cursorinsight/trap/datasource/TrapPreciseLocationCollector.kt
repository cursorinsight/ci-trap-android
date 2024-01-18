package com.cursorinsight.trap.datasource

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapPermissionActivity
import com.cursorinsight.trap.util.TrapTime
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * Collect precise location data and package it
 * as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapPreciseLocationCollector() : TrapDatasource {
    private val locationEventType = 109

    private var storage: SynchronizedQueue<JSONArray>? = null

    private var fusedLocationClient: FusedLocationProviderClient? = null

    private var registered = false

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            locationResult.locations.forEach {
                with(JSONArray()) {
                    put(locationEventType)
                    put(TrapTime.getCurrentTime())
                    put(it.latitude)
                    put(it.longitude)
                    put(it.accuracy)
                    this
                }.let {
                    if (locationResult.locations.size > 0) {
                        storage?.add(it)
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(
        activity: Activity,
        config: TrapConfig.DataCollection,
        storage: SynchronizedQueue<JSONArray>) {
        this.storage = storage
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000
        ).build()
        TrapBackgroundExecutor.run { }.also {
            fusedLocationClient?.requestLocationUpdates(
                locationRequest,
                TrapBackgroundExecutor.executor!!,
                locationCallback
            )
            registered = true
        }
    }

    override fun stop(activity: Activity) {
        if (registered) {
            fusedLocationClient?.removeLocationUpdates(locationCallback)
            registered = false
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
            val preciseOk = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            val coarseOk = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

            return preciseOk && coarseOk
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
                        IntentFilter("com.cursorinsight.trap.FINE_LOCATION_PERMISSION"),
                        Context.RECEIVER_NOT_EXPORTED
                    )
                } else {
                    activity.registerReceiver(
                        receiver,
                        IntentFilter("com.cursorinsight.trap.FINE_LOCATION_PERMISSION")
                    )
                }
                val permissionActivityIntent = Intent(activity, TrapPermissionActivity::class.java)
                permissionActivityIntent.putExtra(
                    "PERMISSION",
                    TrapPermissionActivity.Permissions.FINE_LOCATION.code
                )
                activity.startActivity(permissionActivityIntent)
            } else {
                onSuccess()
            }
        }
    }
}
