package  com.cursorinsight.trap.datasource

import android.app.Activity
import com.cursorinsight.trap.TrapConfig
import org.apache.commons.collections4.queue.SynchronizedQueue
import org.json.JSONArray

/**
 * The interface describing a data source collector which
 * can be added to the platform and it's lifecycle tracked.
 */
interface TrapDatasource {
    /**
     * Start the data collection process for this particular collector with the specified config.
     */
    fun start(
        activity: Activity,
        config: TrapConfig.DataCollection,
        storage: SynchronizedQueue<JSONArray>
    )

    /**
     * Stop the data collection process for this particular collector.
     */
    fun stop(activity: Activity)

    fun getName() : String {
        return this.javaClass.canonicalName ?: ""
    }
}
