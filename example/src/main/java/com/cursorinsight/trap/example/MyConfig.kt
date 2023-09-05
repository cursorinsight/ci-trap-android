package com.cursorinsight.trap.example

import android.app.Application
import com.cursorinsight.trap.TrapConfig
import com.cursorinsight.trap.TrapConfigProvider
import java.io.File
import java.util.UUID

@Suppress("unused")
class MyConfig: TrapConfigProvider {
    override fun getConfig(application: Application): TrapConfig {
        val config = TrapConfig()

        config.reporter.url = "https://example.com/api/post/{streamId}/{sessionId}"

        // Set session id
        val file = File(application.filesDir, "trap-session.id")
        if (!file.exists()) {
            file.writeText(UUID.randomUUID().toString())
        }
        config.reporter.sessionId = UUID.fromString(file.readText())

        return config
    }
}
