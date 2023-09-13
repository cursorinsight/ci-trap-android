package com.cursorinsight.trap.util
import android.os.SystemClock

/**
 * Utility class to calculate the unified timestamps.
 */
internal class TrapTime {
    companion object {
        var bootTime : Long
        var timeDiff: Long

        init {
            bootTime = System.currentTimeMillis() -
                    SystemClock.elapsedRealtime()
            timeDiff = SystemClock.elapsedRealtime() -
                    SystemClock.uptimeMillis()
        }

        /**
         * Brings the nanosecond resolution real time diff into
         * a millisecond epoch.
         *
         * @param time The time diff in ns
         * @return ms epoch
         */
        fun normalizeRealTimeNanosecond(time: Long): Long {
            return bootTime + time.div(1_000_000)
        }

        /**
         * Brings the millisecond resolution up time diff into
         * a millisecond epoch.
         *
         * @param time The time diff in ms
         * @return ms epoch
         */
        fun normalizeUptimeMillisecond(time: Long): Long {
            return bootTime + time + timeDiff
        }

        /**
         * Updates the time difference between the elapsedRealtime
         * and the uptime
         */
        fun updateTimeDiff() {
            timeDiff = SystemClock.elapsedRealtime() -
                    SystemClock.uptimeMillis()
        }

        /**
         * Returns the current time in a millisecond epoch
         * @return ms epoch
         */
        fun getCurrentTime(): Long {
            return bootTime + SystemClock.elapsedRealtime()
        }

    }
}