package com.cursorinsight.trap.datasource

import android.app.Activity

/**
 * The interface describing a data source collector which
 * can be added to the platform and it's lifecycle tracked.
 */
interface TrapDatasource {
    /**
     * Start the data collection process for this particular collector.
     */
    fun start(activity: Activity)

    /**
     * Stop the data collection process for this particular collector.
     */
    fun stop(activity: Activity)
}
