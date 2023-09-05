package com.cursorinsight.trap.transport

/**
 * Represents a generic error for transports
 * which can't be handled by layers above.
 *
 * @property message The message payload of the exception.
 */
internal class TrapTransportException(override val message: String? = null): Exception() {}