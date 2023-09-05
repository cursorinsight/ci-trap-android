package com.cursorinsight.trap.util

import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Common background threadpool for Trap.
 */
internal class TrapBackgroundExecutor {
    companion object {
        /**
         * The common executor pool.
         */
        @Volatile
        var executor: Executor? = null

        /**
         * The scheduled, repeating threadpool.
         */
        @Volatile
        private var schedulerExecutor: ScheduledExecutorService? = null

        /**
         * Run a closure on the background threadpool.
         *
         * @param command The closure to run in the thread pool.
         */
        @Synchronized
        fun run(command: () -> Unit) {
            if (executor == null) {
                executor = Executors.newCachedThreadPool()
            }
            executor!!.execute(command)
        }

        /**
         * Run a task at regular intervals.
         *
         * @param command The closure to run in the thread pool.
         * @param initialDelay How many units to wait before running it the first time.
         * @param period How many units to wait between subsequenct command calls
         * @param unit The time unit to use (i.e. ms, ns, s etc.)
         * @return The Future you can use to cancel this repeating task.
         */
        @Synchronized
        fun runScheduled(
            command: () -> Unit,
            initialDelay: Long,
            period: Long,
            unit: TimeUnit
        ): Future<*> {
            if (schedulerExecutor == null) {
                schedulerExecutor = Executors.newScheduledThreadPool(1)
            }
            return schedulerExecutor!!.scheduleAtFixedRate(command, initialDelay, period, unit)
        }
    }
}