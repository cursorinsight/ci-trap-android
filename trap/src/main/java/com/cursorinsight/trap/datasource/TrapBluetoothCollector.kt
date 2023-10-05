package com.cursorinsight.trap.datasource

import android.Manifest.permission.ACCESS_FINE_LOCATION
import android.Manifest.permission.BLUETOOTH
import android.Manifest.permission.BLUETOOTH_ADMIN
import android.Manifest.permission.BLUETOOTH_CONNECT
import android.Manifest.permission.BLUETOOTH_SCAN
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothDevice.BOND_BONDED
import android.bluetooth.BluetoothDevice.BOND_BONDING
import android.bluetooth.BluetoothDevice.BOND_NONE
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.BLUETOOTH_SERVICE
import android.content.Context.RECEIVER_NOT_EXPORTED
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapPermissionActivity
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import java.lang.reflect.Method

/**
 * Monitors for Bluetooth device cnnections then reports them
 * as data frames.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 *
 * @param config The library config instance.
 */
class TrapBluetoothCollector(
    private val storage: SynchronizedQueue<JSONArray>,
    @Suppress("UNUSED_PARAMETER") config: TrapConfig,
) : TrapDatasource {
    val bluetoothEventType = 108

    private var registered = false

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            try {
                when (intent.action) {
                    BluetoothDevice.ACTION_FOUND -> {
                        val device: BluetoothDevice? =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                intent.getParcelableExtra(
                                    BluetoothDevice.EXTRA_DEVICE,
                                    BluetoothDevice::class.java
                                )
                            } else {
                                @Suppress("DEPRECATION") intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                            }

                        with(JSONArray()) {
                            put(bluetoothEventType)
                            put(System.currentTimeMillis())
                            put(with(JSONArray()) {
                                put(with(JSONArray()) {
                                    put(device?.name)
                                    put(device?.address)
                                    put(
                                        when (device?.bondState) {
                                            BOND_NONE -> 1
                                            BOND_BONDED, BOND_BONDING -> 3
                                            else -> 0
                                        }
                                    )
                                    this
                                })
                                this
                            })
                            this
                        }.let { storage.add(it) }
                    }
                }
            } catch (ex: Exception) {
                Log.e(
                    TrapBluetoothCollector::class.simpleName,
                    "Processing Bluetooth scan result failed",
                    ex
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    override fun start(activity: Activity) {
        if (checkPermissions(activity) && activity.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)) {
            val bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager

            val bondedDevices = bluetoothManager.adapter.bondedDevices.toList()
            with(JSONArray()) {
                put(bluetoothEventType)
                put(System.currentTimeMillis())
                put(with(JSONArray()) {
                    bondedDevices.forEach { device ->
                        put(with(JSONArray()) {
                            put(device?.name ?: "<unknown>")
                            put(device?.address)
                            put(if (isConnected(device)) { 3 } else { 2 })
                            this
                        })
                    }
                    this
                })
            }.let {
                if (it.length() > 0) {
                    storage.add(it)
                }
            }

            activity.registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            registered = true

            val success = bluetoothManager.adapter.startDiscovery()
            if (!success) {
                Log.e(TrapBluetoothCollector::class.simpleName, "Bluetooth scan failed")
            }
        } else {
            Log.w(TrapBluetoothCollector::class.simpleName, "Bluetooth is not supported")
        }
    }

    @SuppressLint("MissingPermission")
    override fun stop(activity: Activity) {
        if (registered) {
            activity.unregisterReceiver(receiver)
            registered = false
            val bluetoothManager = activity.getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
            if (bluetoothManager.adapter.isDiscovering) {
                bluetoothManager.adapter.cancelDiscovery()
            }
        }
    }

    /**
     * Determines if a Bluetooth device is connected or not.
     *
     * @param device The device in question.
     * @return Returns true if the device is connected.
     */
    private fun isConnected(device: BluetoothDevice): Boolean {
        return try {
            val m: Method = device.javaClass.getMethod("isConnected")
            m.invoke(device) as Boolean
        } catch (e: Exception) {
            false
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
            val scanOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(activity, BLUETOOTH_SCAN) == PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(activity, BLUETOOTH) == PERMISSION_GRANTED
            }
            val connectOk = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(activity, BLUETOOTH_CONNECT) == PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(activity, BLUETOOTH_ADMIN) == PERMISSION_GRANTED
            }
            val locationOk = ContextCompat.checkSelfPermission(activity, ACCESS_FINE_LOCATION) == PERMISSION_GRANTED

            return scanOk && connectOk && locationOk
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
                        IntentFilter("com.cursorinsight.trap.BLUETOOTH_PERMISSION"),
                        RECEIVER_NOT_EXPORTED
                    )
                } else {
                    activity.registerReceiver(
                        receiver,
                        IntentFilter("com.cursorinsight.trap.BLUETOOTH_PERMISSION")
                    )
                }
                val permissionActivityIntent = Intent(activity, TrapPermissionActivity::class.java)
                permissionActivityIntent.putExtra(
                    "PERMISSION",
                    TrapPermissionActivity.Permissions.BLUETOOTH.code
                )
                activity.startActivity(permissionActivityIntent)
            } else {
                onSuccess()
            }
        }
    }
}
