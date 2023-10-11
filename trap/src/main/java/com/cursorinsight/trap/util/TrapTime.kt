package com.cursorinsight.trap.util
import android.os.SystemClock

/**
 * Utility class to calculate the unified timestamps.
 */
internal class TrapTime {
    companion object {
        /**
         * Both elapsedRealtime and uptimeMillis are reported relative to the system boot
         * and we have to report all timestamps in UNIX epoch we have to determine the system
         * boot time.
         */
        var bootTime : Long

        /**
         * Difference between the elapsedRealtime and uptimeMillis
         *
         * Both are measured in milliseconds since the system boot.
         * But uptimeMillis does not include the time that the device
         * spent in sleep, so elapsedRealtime can differ from uptimeMillis
         *
         * As we would like to report all events with comparable timestamps
         * the events reported in upltimeMillis has to be converted to real time
         * by increasing it with the timeDiff.
         */
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
         * Brings the millisecond resolution uptime diff into
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
         * and the uptime.
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