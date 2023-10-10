package com.cursorinsight.trap.example

import android.app.Application
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.TrapConfigProvider

@Suppress("unused")
class MyConfig: TrapConfigProvider {
    override fun getConfig(application: Application): TrapConfig {
        val config = TrapConfig()

        config.reporter.url = "https://example.com/api/post/{streamId}/{sessionId}"

        // Set session id
        config.initSessionId(application)

        return config
    }
}
