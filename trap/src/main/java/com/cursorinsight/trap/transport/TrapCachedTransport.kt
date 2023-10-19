package com.cursorinsight.trap.transport

import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapFileCache
import org.json.JSONArray
import java.io.File
import java.net.URI

/**
 * A special transport layer which caches
 * data packets if the underlying transport
 * layer fails and resends the cached packages
 * at the first opportunity.
 *
 * @property underlying The underlying, actual transport to cache.
 * @constructor
 * Set up the transport.
 *
 * @param cacheDir The cache directory on the file system to use.
 * @param cacheSize The quasi-maximum size of the cache.
 */
internal class TrapCachedTransport(
    cacheDir: File,
    cacheSize: Long,
    private val underlying: TrapTransport
) : TrapTransport {
    /**
     * The cache handler instance.
     */
    private val cache = TrapFileCache(cacheDir, cacheSize)

    override fun start(url: URI, config: TrapConfig.Reporter) {
        underlying.start(url, config)
    }

    override fun stop() {
        underlying.stop()
    }

    @Throws(Exception::class)
    @OptIn(ExperimentalStdlibApi::class)
    override fun send(data: String, avoidSendingTooMuchData : Boolean) {
        try {
            // Attempt to send the cache contents
            if (!avoidSendingTooMuchData) {
                val cached = cache.getAll()
                if (cached.isNotEmpty()) {
                    cached.forEach {
                        underlying.send(it.content(), avoidSendingTooMuchData)
                        it.delete()
                    }
                }
            }
            // Attempt to send the current data packet
            underlying.send(data, avoidSendingTooMuchData)
        } catch (_: TrapTransportException) {
            // If sending fails at any point, store the data packet
            // in the cache and get back to the called
            cache.push(data)
        }
    }
}
