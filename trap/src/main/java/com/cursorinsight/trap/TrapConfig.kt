package com.cursorinsight.trap

import android.app.Application
import com.cursorinsight.trap.datasource.TrapBluetoothCollector
import com.cursorinsight.trap.datasource.TrapCoarseLocationCollector
import com.cursorinsight.trap.datasource.TrapMetaDataCollector
import com.cursorinsight.trap.datasource.TrapWiFiCollector
import com.cursorinsight.trap.datasource.gesture.TrapPointerCollector
import com.cursorinsight.trap.datasource.gesture.TrapStylusCollector
import com.cursorinsight.trap.datasource.gesture.TrapTouchCollector
import com.cursorinsight.trap.datasource.hardware.TrapBatteryCollector
import com.cursorinsight.trap.datasource.sensor.TrapAccelerometerCollector
import com.cursorinsight.trap.datasource.sensor.TrapGravityCollector
import com.cursorinsight.trap.datasource.sensor.TrapGyroscopeCollector
import com.cursorinsight.trap.datasource.sensor.TrapMagnetometerCollector
import java.io.File
import java.util.UUID

/**
 * The global configuration for the library.
 */
data class TrapConfig(
    /**
     * The configuration for the reporter task.
     */
    var reporter: Reporter = Reporter(),

    /**
     * Default data collection options.
     */
    var defaultDataCollection: DataCollection = DataCollection(),

    /**
     * Limited data collection in case of low battery
     */
    var lowBatteryDataCollection: DataCollection = DataCollection(
        collectCoalescedPointerEvents = false,
        collectCoalescedStylusEvents = false,
        collectCoalescedTouchEvents = false,
        collectors = mutableListOf(
            TrapMetaDataCollector::class.qualifiedName,
            TrapCoarseLocationCollector::class.qualifiedName,
            TrapPointerCollector::class.qualifiedName,
            TrapStylusCollector::class.qualifiedName,
            TrapTouchCollector::class.qualifiedName,
            TrapBatteryCollector::class.qualifiedName).filterNotNull()
    ),

    /**
     * Limited data collection in case of low battery
     */
    var lowDataDataCollection: DataCollection = DataCollection(
        collectCoalescedPointerEvents = false,
        collectCoalescedStylusEvents = false,
        collectCoalescedTouchEvents = false,
        collectors = mutableListOf(
            TrapMetaDataCollector::class.qualifiedName,
            TrapCoarseLocationCollector::class.qualifiedName,
            TrapPointerCollector::class.qualifiedName,
            TrapStylusCollector::class.qualifiedName,
            TrapTouchCollector::class.qualifiedName,
            TrapBatteryCollector::class.qualifiedName).filterNotNull()
    ),

    /**
     * The size of the circular data queue.
     */
    var queueSize: Int = 2048,

) {
    /**
     * The configuration for the reporter task serializing
     * and sending the collected data through the transport.
     */
    data class Reporter(
        /**
         * The URL to send the data packets to.
         */
        var url: String = "https://example.com/api/post/{streamId}/{sessionId}",

        /**
         * The time interval the reporter task runs with.
         */
        var interval: Long = 1000,

        /**
         * Whether to cache data packets on the device
         * when connection to the remote server cannot be
         * established.
         */
        var cachedTransport: Boolean = true,

        /**
         * About how much space on the device can be
         * used to store unsent data packets.
         *
         * The lib might use a little more space than this
         * value in case the data packet size exceeds the
         * remaining space.
         */
        var maxFileCacheSize: Long = 5_000_000,

        /**
         * The persistent session id to send in the
         * header frame. Must be set manually
         * with a custom config class!
         */
        var sessionId: UUID = UUID(0, 0),

        /**
         * The connect timeout for the HTTP transport
         * in milliseconds.
         */
        var connectTimeout: Int = 500,

        /**
         * The read timeout for the HTTP transport
         * in milliseconds.
         */
        var readTimeout: Int = 500,

        /**
         * Whether to compress the data sent to the server.
         * If true GZIP compression is used.
         */
        var compress: Boolean = false,

        /**
         * Name of the api key sent in the HTTP header
         */
        var apiKeyName: String = "graboxy-api-key",

        /**
         * Value of the api key sent in the HTTP header
         */
        var apiKeyValue: String = "api-key-value"
    )

    data class DataCollection(

        /**
         * Maximum number of log messages per collector if the collector uses log throttling
         */
        var maxNumberOfLogMessagesPerMinute: Int = 100,

        /**
         * How frequent the sampling of the given sensor should be.
         */
        var accelerationSamplingPeriodMs: Int = 10,

        /**
         * How long the sensor can cache reported events.
         */
        var accelerationMaxReportLatencyMs: Int = 200,

        /**
         * How frequent the sampling of the given sensor should be.
         */
        var gyroscopeSamplingPeriodMs: Int = 10,

        /**
         * How long the sensor can cache reported events.
         */
        var gyroscopeMaxReportLatencyMs: Int = 200,

        /**
         * How frequent the sampling of the given sensor should be.
         */
        var magnetometerSamplingPeriodMs: Int = 10,

        /**
         * How long the sensor can cache reported events.
         */
        var magnetometerMaxReportLatencyMs: Int = 200,

        /**
         * How frequent the sampling of the given sensor should be.
         */
        var gravitySamplingPeriodMs: Int = 10,

        /**
         * How long the sensor can cache reported events.
         */
        var gravityMaxReportLatencyMs: Int = 200,

        /**
         * Collect coalesced pointer events
         */
        var collectCoalescedPointerEvents: Boolean = true,

        /**
         * Collect coalesced stylus events
         */
        var collectCoalescedStylusEvents: Boolean = true,

        /**
         * Collect coalesced touch events
         */
        var collectCoalescedTouchEvents: Boolean = true,

        /**
         * The time interval metadata events are reported.
         */
        var metadataSubmissionInterval: Long = 60_000,

        /**
         * The list of collectors to start at initialization.
         */
        var collectors: List<String> = mutableListOf(
            TrapAccelerometerCollector::class.qualifiedName,
            TrapBluetoothCollector::class.qualifiedName,
            TrapGravityCollector::class.qualifiedName,
            TrapGyroscopeCollector::class.qualifiedName,
            TrapCoarseLocationCollector::class.qualifiedName,
            TrapMagnetometerCollector::class.qualifiedName,
            TrapPointerCollector::class.qualifiedName,
            TrapStylusCollector::class.qualifiedName,
            TrapTouchCollector::class.qualifiedName,
            TrapWiFiCollector::class.qualifiedName,
            TrapBatteryCollector::class.qualifiedName,
            TrapMetaDataCollector::class.qualifiedName,
        ).filterNotNull(),
    )

    fun initSessionId(application: Application) {
        val file = File(application.filesDir, "my-session.id")
        if (!file.exists()) {
            file.writeText(UUID.randomUUID().toString())
        }
        reporter.sessionId = UUID.fromString(file.readText())
    }
}
