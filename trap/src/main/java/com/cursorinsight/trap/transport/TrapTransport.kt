package com.cursorinsight.trap.transport

import java.net.URI
import kotlin.jvm.Throws

/**
 * Represents a generic transport protocol through
 * which the data packets can be sent over.
 */
internal interface TrapTransport {
    /**
     * Start the transport mechanism if it
     * is needed. Must be called before 'send'
     * is called first.
     *
     * @param url The url to initialize this transport method with.
     */
    fun start(url: URI)

    /**
     * Terminate this transport mechanism.
     */
    fun stop()

    /**
     * Send a data packet to the server.
     *
     * @param data The contents to send to the server.
     */
    @Throws(TrapTransportException::class)
    fun send(data: String)
}