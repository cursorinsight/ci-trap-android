package com.cursorinsight.trap.transport

import android.util.Log
import java.net.URI
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.Exception

/**
 * The websocket transport layer implementation, which handles
 * disconnects and errors with continuous reconnect attempts and
 * keeps the connection alive with ping control frames if there is
 * no data packets sent within 3 seconds.
 */
internal class TrapWebsocketTransport(): TrapTransport {
    private var url: URI? = null

    /**
     * The websocket instance we use to send data packets.
     */
    private var websocket: WebSocket? = null

    /**
     * The task handler for the repeating ping task.
     */
    private var pingTask: Future<*>? = null

    override fun start(url: URI) {
        assert(url.scheme.startsWith("ws"))
        this.url = url
        websocket = WebSocket(url)
        websocket?.connectBlocking(500, TimeUnit.MILLISECONDS)
        schedulePing()
    }

    override fun stop() {
        websocket?.close()
        websocket = null
    }

    override fun send(data: String) {
        if (websocket == null) {
            Log.d(TrapWebsocketTransport::class.simpleName, "send(): Attempting to reconnect")
            url?.run { start(this) }
        }

        try {
            websocket?.send(data)
            schedulePing()
        } catch (ex: Exception) {
            Log.d(TrapWebsocketTransport::class.simpleName, "send(): Failure to send, terminate connection")
            stop()
            throw TrapTransportException()
        }
    }

    /**
     * Schedule a ping control frame within the next 3 secs.
     */
    private fun schedulePing() {
        pingTask?.cancel(true)
        pingTask = Executors.newSingleThreadScheduledExecutor()
            .scheduleWithFixedDelay({
                websocket?.sendPing()
            }, 3000, 3000, TimeUnit.MILLISECONDS)
    }

    /**
     * The WebSocketClient dummy implementation required by JavaWebSocket.
     */
    private class WebSocket(url: URI): WebSocketClient(url) {
        override fun onOpen(handshakedata: ServerHandshake?) { }

        override fun onMessage(message: String?) { }

        override fun onClose(code: Int, reason: String?, remote: Boolean) { }

        override fun onError(ex: Exception?) {
            Log.w(WebSocket::class.simpleName, "onError(): ${ex?.message}", ex)
        }
    }
}
