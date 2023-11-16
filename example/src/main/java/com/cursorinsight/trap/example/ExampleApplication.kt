package com.cursorinsight.trap.example

import android.app.Application
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.TrapManager
import org.json.JSONObject

class ExampleApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        val config = TrapConfig()

// Can be a websocket or http server
        config.reporter.url = "https://example.com/api/post/{sessionId}/{streamId}"

// Set reporting interval to 3 seconds
        config.reporter.interval = 3000

// Set session id
        config.initSessionId(this)

// Instantiate TrapManager
        val trapManager = TrapManager.getInstance(this, config)

// Add a custom metadata
        trapManager.addCustomMetadata(
            "uid",
            with(JSONObject()) {
                put("type", "text")
                put("value", "uid-text")
                this
            })

        trapManager.addCustomMetadata("someRandomMeta", "textValue")

// Add a custom event
        trapManager.addCustomEvent(with(JSONObject()) {
            put("some-key", "some-data")
            put("numeric-data-key", 2)
            put("boolean-data-key", false)
            this
        })
    }
}