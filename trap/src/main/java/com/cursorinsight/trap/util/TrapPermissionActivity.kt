package com.cursorinsight.trap.util

import android.Manifest.permission.ACCESS_COARSE_LOCATION
import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.ACCESS_NETWORK_STATE
import android.Manifest.permission.ACCESS_WIFI_STATE
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.Manifest.permission.CHANGE_WIFI_STATE
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log

/**
 * A transparent android Activity which can launch permission requests
 * and handle the user response to these permission requests.
 *
 * The activity only handles specific permission patterns for bluetooth, location and wifi.
 */
class TrapPermissionActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val permissionCode = intent.getIntExtra("PERMISSION", 0)
        val permissions: Array<String> = when (permissionCode) {
            Permissions.BLUETOOTH.code -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                arrayOf(BLUETOOTH_SCAN, BLUETOOTH_CONNECT, ACCESS_FINE_LOCATION)
            } else {
                arrayOf(BLUETOOTH, BLUETOOTH_ADMIN, ACCESS_FINE_LOCATION)
            }
            Permissions.FINE_LOCATION.code -> arrayOf(ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION)
            Permissions.COARSE_LOCATION.code -> arrayOf(ACCESS_COARSE_LOCATION)
            Permissions.WIFI.code -> arrayOf(ACCESS_NETWORK_STATE, ACCESS_WIFI_STATE, CHANGE_WIFI_STATE, ACCESS_FINE_LOCATION)
            else -> {
                Log.e(TrapPermissionActivity::class.simpleName, "Unknown permission requested")
                arrayOf()
            }
        }
        requestPermissions(permissions, permissionCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            Permissions.BLUETOOTH.code -> Intent().also { intent ->
                intent.action = "com.cursorinsight.trap.BLUETOOTH_PERMISSION"
                sendBroadcast(intent)
            }
            Permissions.FINE_LOCATION.code -> Intent().also { intent ->
                intent.action = "com.cursorinsight.trap.FINE_LOCATION_PERMISSION"
                sendBroadcast(intent)
            }
            Permissions.COARSE_LOCATION.code -> Intent().also { intent ->
                intent.action = "com.cursorinsight.trap.COARSE_LOCATION_PERMISSION"
                sendBroadcast(intent)
            }
            Permissions.WIFI.code -> Intent().also { intent ->
                intent.action = "com.cursorinsight.trap.WIFI_PERMISSION"
                sendBroadcast(intent)
            }
            else -> Log.e(TrapPermissionActivity::class.simpleName, "Unknown permission requested")
        }

        finish()
    }

    /**
     * The permission request codes we use to identify which permission dialog to handle.
     *
     * @property code Just a random int code which is unique.
     */
    enum class Permissions(val code: Int) {
        BLUETOOTH(22415),
        FINE_LOCATION(23532),
        COARSE_LOCATION(234331),
        WIFI(21445),
    }
}
