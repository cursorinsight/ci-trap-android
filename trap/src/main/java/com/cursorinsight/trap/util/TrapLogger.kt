package com.cursorinsight.trap.util

import android.util.Log
import java.io.File
import java.util.UUID

/**
 * Logger used to throttle log messages
 */
internal class TrapLogger {
    fun logException(tag: String?, msg: String?, exception: Exception) {
        Log.e(tag, msg, exception)
    }
}
