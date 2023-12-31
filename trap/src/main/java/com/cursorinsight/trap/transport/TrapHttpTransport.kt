package com.cursorinsight.trap.transport

import android.util.Log
import com.cursorinsight.trap.TrapConfig
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import java.util.zip.DeflaterOutputStream
import javax.net.ssl.HttpsURLConnection

/**
 * The HTTP transport layer implementation, which POSTs
 * the data packets for every send.
 */
internal class TrapHttpTransport : TrapTransport {
    /**
     * The url to send the data to.
     */
    private var url: URI? = null

    /**
     * Additional configuration properties
     */
    private var config: TrapConfig.Reporter? = null

    override fun start(url: URI, config: TrapConfig.Reporter) {
        assert(url.scheme.startsWith("http"))
        this.url = url
        this.config = config
    }


    override fun stop() {
        url = null
        config = null
    }

    @Throws(
        IOException::class,
        TrapTransportException::class
    )
    override fun send(data: String, avoidSendingTooMuchData: Boolean) {
        val url = url ?: throw TrapTransportException("URL is not valid")
        val config = config ?: throw TrapTransportException("Config is not valid")
        val raw = url.toURL().openConnection()
        val connection = if (url.scheme.equals("http")) {
            raw as HttpURLConnection
        } else {
            raw as HttpsURLConnection
        }

        val compressEncoding = if (config.compress) "+zlib" else ""
        connection.setRequestProperty("Content-Type", "text/plain; encoding=json$compressEncoding")
        connection.setRequestProperty(config.apiKeyName, config.apiKeyValue)
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.doInput = false
        connection.connectTimeout = config.connectTimeout
        connection.readTimeout = config.readTimeout

        with(connection) {
            try {
                if (config.compress) {
                    DeflaterOutputStream(outputStream).use {
                        val writer = BufferedWriter(OutputStreamWriter(it, "UTF-8"))
                        writer.write(data)
                        writer.flush()
                    }
                }
                else {
                    val writer = BufferedWriter(
                        OutputStreamWriter(
                            BufferedOutputStream(outputStream),
                            "UTF-8"
                        )
                    )
                    writer.write(data)
                    writer.flush()
                }
                if (responseCode !in 200..299) {
                    Log.w(
                        TrapHttpTransport::class.simpleName,
                        "POST failed with code $responseCode"
                    )
                    throw TrapTransportException()
                }
                return
            } catch (ex: IOException) {
                // Ignore connection errors, we'll handle them in the finally block.
                // Note: Can't catch SocketTimeoutException, so resorting to this.
                if (!(ex.message?.startsWith("failed to connect to")  == true) &&
                    !(ex.message?.startsWith("timeout") == true)
                ) {
                    Log.e(TrapHttpTransport::class.simpleName, "Unknown IOException happened", ex)
                }
                throw TrapTransportException()
            } finally {
                disconnect()
            }
        }
    }
}
