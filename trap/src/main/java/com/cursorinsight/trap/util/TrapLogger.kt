package com.cursorinsight.trap.util
import android.util.Log


/**
 * Logger used to throttle log messages
 */
internal class TrapLogger(
    maxNumberOfMessagesPerMinute: Int
) {
    private val timeIntervalMs: Long = 60_000 //1 minute
    private val averageTimeBetweenMessages = timeIntervalMs / maxNumberOfMessagesPerMinute
    private var score: Long = 0

    fun throttle(): Boolean {
        var newScore = score
        val now = TrapTime.getCurrentTime()
        if (score == 0L) {
            newScore = now + averageTimeBetweenMessages
        } else {
            newScore += averageTimeBetweenMessages
        }
        if (newScore < now) {
            newScore = now + averageTimeBetweenMessages
        } else if (newScore > now + timeIntervalMs) {
            return false
        }
        score = newScore
        return true
    }

    fun logException(tag: String?, msg: String?, exception: Exception) {
        if (throttle()) {
            Log.e(tag, msg, exception)
        }
    }
}
