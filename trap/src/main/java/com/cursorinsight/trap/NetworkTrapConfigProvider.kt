package com.cursorinsight.trap

import android.app.Application
import android.util.Log
import com.google.gson.Gson
import java.net.HttpURLConnection
import java.net.URL

open class NetworkTrapConfigProvider(
    private var configUrl: URL
) : TrapConfigProvider {
    override fun getConfig(application: Application): TrapConfig {
        try {
            val configString = downloadConfig()
            return Gson().fromJson(configString, TrapConfig::class.java)
        } catch (e: Exception) {
            Log.e(
                NetworkTrapConfigProvider::class.simpleName,
                "Could not download or parse configuration",
                e
            )
        }
        return TrapConfig()
    }

    protected fun downloadConfig() : String {
        val conn = configUrl.openConnection() as HttpURLConnection
        with(conn) {
            requestMethod = "GET"
        }
        val responseBody = conn.inputStream.use { it.readBytes() }.toString(Charsets.UTF_8)
        return responseBody
    }
}