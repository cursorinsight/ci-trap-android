package com.cursorinsight.trap.transport

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

    override fun start(url: URI) {
        underlying.start(url)
    }

    override fun stop() {
        underlying.stop()
    }

    @Throws(Exception::class)
    override fun send(data: String) {
        try {
            // Attempt to send the cache contents
            val cached = cache.getAll()
            if (cached.isNotEmpty()) {
                val json = cached
                    .map { JSONArray(it.content()) }
                    .reduce { acc, jsonArray ->
                        if (jsonArray.length() > 0) {
                            for (i in 0..<jsonArray.length()) {
                                acc.put(jsonArray.get(i))
                            }
                        }
                        acc
                    }
                underlying.send(json.toString())
                cached.forEach { it.delete() }
            }

            // Attempt to send the current data packet
            underlying.send(data)
        } catch (_: TrapTransportException) {
            // If sending fails at any point, store the data packet
            // in the cache and get back to the called
            cache.push(data)
        }
    }
}
