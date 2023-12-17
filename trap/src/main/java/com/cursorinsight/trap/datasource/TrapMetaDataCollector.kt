package com.cursorinsight.trap.datasource

import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.media.AudioManager
import android.net.wifi.WifiManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import android.webkit.WebSettings
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Collect device specific metadata and send it as data frames periodically.
 *
 * @property storage The data frame queue.
 * @constructor
 * Sets up the data collector.
 */
class TrapMetadataCollector (
    private val storage: SynchronizedQueue<JSONArray>
) : TrapDatasource {
    private val metadataEventType = 11

    private val customJSONObject: JSONObject = JSONObject()

    private var context: Context? = null

    private var task: Future<*>? = null

    override fun start(activity: Activity, config: TrapConfig.DataCollection) {
        if (task == null) {
            context = activity.applicationContext
            task = TrapBackgroundExecutor.runScheduled({
                sendMetadataEvent()
            }, 0, config.metadataSubmissionInterval, TimeUnit.MILLISECONDS)
        }
    }

    override fun stop(activity: Activity) {
        task?.cancel(true)
        task = null
    }

    @Suppress("unused")
    fun addCustom(key: String, value: Any) {
        customJSONObject.put(key, value)
        if (task != null) {
            sendMetadataEvent()
        }
    }

    @Suppress("unused")
    fun removeCustom(key: String) {
        customJSONObject.remove(key)
        if (task != null) {
            sendMetadataEvent()
        }
    }

    private fun sendMetadataEvent() {
        try {
            storage.add(with(JSONArray())
            {
                put(metadataEventType)
                put(TrapTime.getCurrentTime())
                put(with(JSONObject()) {
                    put("build", buildData())
                    put("storage", storageData())
                    put("custom", customJSONObject)
                    put("screen", screenData())
                    put("hardware", hardwareData())
                    this
                })
                this
            })
        } catch (ex: Exception) {
            Log.e(
                TrapMetadataCollector::class.simpleName,
                "Sending metadata event failed",
                ex
            )
        }
    }

    private fun buildData() : JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("deviceName", deviceName)
            jsonObject.put("osVersion", osVersion)
            jsonObject.put("buildVersionCodeName", buildVersionCodeName)
            jsonObject.put("product", product)
            jsonObject.put("fingerprint", fingerprint)
            jsonObject.put("hardware", hardware)
            jsonObject.put("radioVersion", radioVersion)
            jsonObject.put("device", device)
            jsonObject.put("board", board)
            jsonObject.put("displayVersion", displayVersion)
            jsonObject.put("buildBrand", buildBrand)
            jsonObject.put("buildHost", buildHost)
            jsonObject.put("buildTime", buildTime)
            jsonObject.put("buildUser", buildUser)
            jsonObject.put("osVersion", osVersion)
            jsonObject.put("sdkVersion", sdkVersion)
            return jsonObject
        } catch (ex: Exception) {
            Log.e(
                TrapMetadataCollector::class.simpleName,
                "Collecting build data failed",
                ex
            )
        }
        return JSONObject()
    }

    private fun storageData() : JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("totalRAM", totalRAM)
            jsonObject.put("totalExternalMemorySize", totalExternalMemorySize)
            jsonObject.put("availableExternalMemorySize", availableExternalMemorySize)
            jsonObject.put("totalInternalMemorySize", totalInternalMemorySize)
            jsonObject.put("availableInternalMemorySize", availableInternalMemorySize)
            return jsonObject
        } catch (ex: Exception) {
            Log.e(
                TrapMetadataCollector::class.simpleName,
                "Collecting storage data failed",
                ex
            )
        }
        return JSONObject()
    }

    private fun screenData() : JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("screenDensity", screenDensity)
            jsonObject.put("screenHeight", screenHeight)
            jsonObject.put("screenWidth", screenWidth)
            jsonObject.put("orientation", orientation)
            return jsonObject
        } catch (ex: Exception) {
            Log.e(
                TrapMetadataCollector::class.simpleName,
                "Collecting screen data failed",
                ex
            )
        }
        return JSONObject()
    }

    private fun hardwareData() : JSONObject {
        try {
            val jsonObject = JSONObject()
            jsonObject.put("androidId", androidId)
            jsonObject.put("locale", deviceLocale)
            jsonObject.put("userAgent", userAgent)
            jsonObject.put("deviceRingerMode", deviceRingerMode)
            jsonObject.put("phoneType", phoneType)
            jsonObject.put("operator", operator)
            jsonObject.put("isSimNetworkLocked", isSimNetworkLocked)
            jsonObject.put("isNfcEnabled", isNfcEnabled)
            jsonObject.put("isNfcPresent", isNfcPresent)
            jsonObject.put("isWifiEnabled", isWifiEnabled)
            return jsonObject
        } catch (ex: Exception) {
            Log.e(
                TrapMetadataCollector::class.simpleName,
                "Collecting hardware data failed",
                ex
            )
        }
        return JSONObject()
    }

    private val deviceName: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model
            } else {
                "$manufacturer $model"
            }
        }

    private val deviceLocale: String?
        get() {
            return context?.resources?.configuration?.locales?.toLanguageTags()
        }

    private val buildVersionCodeName: String
        get() = Build.VERSION.CODENAME

    private val product: String
        get() = Build.PRODUCT

    private val fingerprint: String
        get() = Build.FINGERPRINT

    private val hardware: String
        get() = Build.HARDWARE

    private val radioVersion: String
        get() = Build.getRadioVersion()

    private val device: String
        get() = Build.DEVICE

    private val board: String
        get() = Build.BOARD

    private val displayVersion: String
        get() = Build.DISPLAY

    private val buildBrand: String
        get() = Build.BRAND

    private val buildHost: String
        get() = Build.HOST

    private val buildTime: Long
        get() = Build.TIME

    private val buildUser: String
        get() = Build.USER

    private val osVersion: String
        get() = Build.VERSION.RELEASE

    private val sdkVersion: Int
        get() = Build.VERSION.SDK_INT

    private val screenDensity: Float
        get() {
            return Resources.getSystem().displayMetrics.density
        }

    private val screenHeight: Int
        get() {
            return Resources.getSystem().displayMetrics.heightPixels
        }

    private val screenWidth: Int
        get() {
            return Resources.getSystem().displayMetrics.widthPixels
        }

    private val orientation: Int
        get() {
            return when (context?.resources?.configuration?.orientation) {
                Configuration.ORIENTATION_LANDSCAPE -> 0
                Configuration.ORIENTATION_PORTRAIT -> 1
                else -> -1
            }
        }

    private val deviceRingerMode: Int
        get() {
            val audioManager = context?.getSystemService(Context.AUDIO_SERVICE) as AudioManager?
            return when (audioManager?.ringerMode) {
                AudioManager.RINGER_MODE_SILENT -> 0
                AudioManager.RINGER_MODE_VIBRATE -> 1
                else -> 2
            }
        }

    private val userAgent: String
        get() {
            val systemUa = System.getProperty("http.agent")
            return WebSettings.getDefaultUserAgent(context) + "__" + systemUa
        }

    private val totalRAM: Long
        get() {
            val mi = ActivityManager.MemoryInfo()
            val activityManager = context?.getSystemService(Activity.ACTIVITY_SERVICE) as ActivityManager?
            activityManager?.getMemoryInfo(mi)
            return mi.totalMem
        }

    private val availableInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            return getAvailableSize(path)
        }

    private val totalInternalMemorySize: Long
        get() {
            val path = Environment.getDataDirectory()
            return getAvailableSize(path)
        }

    private val availableExternalMemorySize: Long
        get() {
            if (hasExternalSDCard()) {
                val path = Environment.getExternalStorageDirectory()
                return getAvailableSize(path)
            }
            return 0
        }

    private fun getAvailableSize(path: File): Long {
        val stat = StatFs(path.path)
        return stat.blockSizeLong * stat.availableBlocksLong
    }

    private val totalExternalMemorySize: Long
        get() {
            if (hasExternalSDCard()) {
                val path = Environment.getExternalStorageDirectory()
                val stat = StatFs(path.path)
                return stat.blockSizeLong * stat.blockCountLong
            }
            return 0
        }

    private val androidId: String?
        get() = if (context != null) {
            Settings.Secure.getString(
                context?.contentResolver,
                Settings.Secure.ANDROID_ID
            )
        } else {
            null
        }

    private val phoneType: String
        get() {
            val tm = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            return when (tm?.phoneType) {
                TelephonyManager.PHONE_TYPE_GSM -> "GSM"
                TelephonyManager.PHONE_TYPE_CDMA -> "CDMA"
                TelephonyManager.PHONE_TYPE_NONE -> "NONE"
                else -> "NONE"
            }
        }

    private val operator: String?
        get() {
            var operatorName: String?
            val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            operatorName = telephonyManager?.networkOperatorName
            if (operatorName == null)
                operatorName = telephonyManager?.simOperatorName
            return operatorName
        }

    private val isSimNetworkLocked: Boolean
        get() {
            val telephonyManager = context?.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager?
            return telephonyManager?.simState == TelephonyManager.SIM_STATE_NETWORK_LOCKED
        }

    private val isNfcPresent: Boolean
        get() {
            if (context != null) {
                val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                return nfcAdapter != null
            } else {
                return false
            }
        }

    private val isNfcEnabled: Boolean
        get() {
            if (context != null) {
                val nfcAdapter = NfcAdapter.getDefaultAdapter(context)
                return nfcAdapter != null && nfcAdapter.isEnabled
            } else {
                return false
            }
        }

    private val isWifiEnabled: Boolean
        get() {
            val wifiManager = context?.getSystemService(Context.WIFI_SERVICE) as WifiManager?
            return wifiManager?.isWifiEnabled ?: false
        }

    private fun hasExternalSDCard(): Boolean {
        return Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
    }
}
