package com.cursorinsight.trap.transport

import android.util.Log
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URI
import javax.net.ssl.HttpsURLConnection

/**
 * The HTTP transport layer implementation, which POSTs
 * the data packets for every send.
 *
 * @property connectTimeout The maximum time to wait for the connection to establish.
 * @property readTimeout The maximum time to wait for a response from the server.
 */
internal class TrapHttpTransport(
    private val connectTimeout: Int,
    private val readTimeout: Int,
) : TrapTransport {
    /**
     * The url to send the data to.
     */
    private var url: URI? = null

    override fun start(url: URI) {
        assert(url.scheme.startsWith("http"))
        this.url = url
    }


    override fun stop() {
        url = null
    }

    @Throws(
        IOException::class,
        TrapTransportException::class
    )
    override fun send(data: String) {
        val url = url ?: throw TrapTransportException("URL is not valid")

        val raw = url.toURL().openConnection()
        val connection = if (url.scheme.equals("http")) {
            raw as HttpURLConnection
        } else {
            raw as HttpsURLConnection
        }

        connection.setRequestProperty("Content-Type", "text/plain; encoding=json")
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.doInput = false
        connection.connectTimeout = connectTimeout
        connection.readTimeout = readTimeout

        with(connection) {
            try {
                val writer =
                    BufferedWriter(OutputStreamWriter(BufferedOutputStream(outputStream), "UTF-8"))
                writer.write(data)
                writer.flush()

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
                if (ex.message?.startsWith("failed to connect to") == true
                    || ex.message?.startsWith("timeout") == true
                ) {
                    throw TrapTransportException()
                } else {
                    Log.e(TrapHttpTransport::class.simpleName, "Unknown IOException happened", ex)
                }
            } finally {
                disconnect()
            }
        }
    }
}
