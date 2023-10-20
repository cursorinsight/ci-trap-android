package com.cursorinsight.trap.transport

import android.content.Context
import android.util.Log
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.util.TrapBackgroundExecutor
import com.cursorinsight.trap.util.TrapTime
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray
import org.json.JSONObject
import java.net.URI
import java.util.UUID
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


/**
 * The reporting system which collects the data from the buffer
 * and invokes the transport to send it to the server.
 *
 * @property config The library config.
 * @property ctx The android Context instance.
 * @property storage The data frame queue buffer.
 */
internal class TrapReporter(
    private val config: TrapConfig,
    private val ctx: Context,
    private val storage: SynchronizedQueue<JSONArray>
) {
    /**
     * The transport layer to use to send data packets through.
     */
    private var transport: TrapTransport? = null

    /**
     * The regular task which collects and sends the data packet through the transport layer.
     */
    private var task: Future<*>? = null

    /**
     * The sessionId set in the config.
     */
    private val sessionId = config.reporter.sessionId

    /**
     * The streamId to send in the header or url.
     */
    private var streamId = UUID.randomUUID()

    /**
     * The sequenceId of the data packet.
     */
    private var sequenceId: Long = 0

    /**
     * Start the reporter task and all
     * necessary underlying systems.
     */
    fun start(avoidSendingTooMuchData: Boolean = false) {
        if (task == null) {
            val url = URI(
                config.reporter.url
                    .replace("{streamId}", streamId.toString(), true)
                    .replace("{sessionId}", sessionId.toString(), true)
            )
            val underlyingTransport = when (url.scheme) {
                "http", "https" -> TrapHttpTransport()

                "ws", "wss" -> TrapWebsocketTransport()
                else -> throw IllegalArgumentException("Unknown transport scheme $url")
            }
            transport = if (config.reporter.cachedTransport) {
                TrapCachedTransport(
                    ctx.cacheDir,
                    config.reporter.maxFileCacheSize,
                    underlyingTransport
                )
            } else {
                underlyingTransport
            }
            transport?.start(url, config.reporter)

            task = TrapBackgroundExecutor.runScheduled({
                // Safety first: In case the background reporter
                // will run in a concurrent setting by mistake.
                //
                // Note: Can't just .toArray() and .clear() because
                // the collectors are running on separate threads and
                // they are not synchronized with the reporter thread,
                // while between the .toArray() and .clear() calls
                // the lock is released.
                val packet: JSONArray = synchronized(storage) {
                    val packet = mutableListOf<JSONArray>()
                    while (!storage.isEmpty()) {
                        storage.poll()?.let { packet.add(it) }
                    }

                    packet.sortBy { it.get(1) as? Long }

                    packet.add(0, header())
                    JSONArray(packet)
                }

                try {
                    if (packet.length() > 1) {
                        transport?.send(
                            packet.toString(),
                            avoidSendingTooMuchData
                        )
                    }
                } catch (ex: Exception) {
                    // There is nowhere to go if we get here, the user can't do anything
                    // and the data have nowhere to go.
                    Log.e(TrapReporter::class.simpleName, "TrapReporter send failed", ex)
                }
            }, config.reporter.interval, config.reporter.interval, TimeUnit.MILLISECONDS)
        }
    }

    /**
     * Stop the reporter task and all
     * necessary underlying systems.
     */
    fun stop() {
        task?.cancel(true)
        task = null
        transport?.stop()
    }

    /**
     * Generates the header frame for the next
     * data packet.
     *
     * @return The header data frame.
     */
    private fun header(): JSONArray {
        val headerEventType = -1
        return with(JSONArray()) {
            put(headerEventType)
            put(TrapTime.getCurrentTime())
            put(sessionId.toString())
            put(streamId.toString())
            put(sequenceId++)
            put(with(JSONObject()) {
                put("version", "20230706T094422Z")
                this
            })
            this
        }
    }
}
