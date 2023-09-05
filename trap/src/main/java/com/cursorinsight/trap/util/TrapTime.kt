package com.cursorinsight.trap.util

/**
 * Utility class to calculate the unified timestamps.
 */
internal class TrapTime {
    companion object {
        var bootTime: Long = System.currentTimeMillis()

        /**
         * Brings the nanosecond resolution time diff into
         * a millisecond epoch.
         *
         * @param time The time diff in ns
         * @return ms epoch
         */
        fun normalizeNanosecondTime(time: Long): Long {
            return bootTime + time.div(1_000_000)
        }

        /**
         * Brings the millisecond resolution time diff into
         * a millisecond epoch.
         *
         * @param time The time diff in ms
         * @return ms epoch
         */
        fun normalizeMillisecondTime(time: Long): Long {
            return bootTime + time
        }
    }
}