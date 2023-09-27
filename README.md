# Trap Library for Android
This library can collect various device and user data, forwarding it to a specified endpoint. The following data collectors are bundled with this version:

* Accelerometer
* Gravity
* Gyroscope
* Magnetometer
* Bluetooth LE devices connected / peered (Needs interactive permission, namely precise location)
* Approximate and precise location (Needs interactive permission)
* WiFi connection and available networks (Needs interactive permissions, namely precise location)
* Touch
* Indirect pointer (mouse)
* Pencil and stylus
* Tap gesture

## How to use it

### Option A - Using a custom Application implementation

You can check out the Example app in this project for a working example. The application needs to 
either use the provided TrapApplication as the `android:name` value for the `<application>` tag in 
the AndroidManifest.xml, or subclass TrapApplication if you also need to use a custom Application 
class. Configuration is done via a `<meta-data>` tag within the `<application>` tag.

#### 1. Specify the application class
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name="com.cursorinsight.trap.TrapApplication"
    >
        <activity
            android:name=".MainActivity"
        >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

#### 2. (Optional) Use a custom config class
Create the configuration provider implementation.

```kotlin
package com.example.application

import android.app.Application
import java.io.File
import java.net.URI
import java.util.UUID

@Suppress("unused")
class MyConfig: TrapConfigProvider {
    override fun getConfig(application: Application): TrapConfig {
        val config = TrapConfig()

        // Can be a websocket or http server
        config.reporter.url = "https://my.server.com"
        
        // Use a special set of data collectors
        config.collectors = mutableListOf(TrapCoarseLocationCollector::class)

        // Set session id
        val file = File(application.filesDir, "my-session.id")
        if (!file.exists()) {
            file.writeText(UUID.randomUUID().toString())
        }
        config.reporter.sessionId = UUID.fromString(file.readText())

        return config
    }
}
```

Specify your configuration provider in the `AndroidManifest.xml`.
```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <application
        android:name="com.cursorinsight.trap.TrapApplication"
    >
        <meta-data android:name="trap:config" android:value="com.cursorinsight.trap.MyConfig" />

        <activity
            android:name=".MainActivity"
        >
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>
    </application>

</manifest>
```

### Option B - Intialize TrapManager from code

Execute the following code during application startup. Preferably in the `onCreate` method of the 
Application, by subclassing the default Android Application implementation. An alternative option 
can be to execute the code  in the `onCreate` method of the main activity of the application, before ˛
calling `super.onCreate()`.

```kotlin
val config = TrapConfig()
config.reporter.url = "https://example.com/api/post/{streamId}/{sessionId}"

// Use a special set of data collectors
config.collectors = mutableListOf(TrapCoarseLocationCollector::class)

// Set session id
val file = File(application.filesDir, "my-session.id")
if (!file.exists()) {
    file.writeText(UUID.randomUUID().toString())
}
config.reporter.sessionId = UUID.fromString(file.readText())

// Set session id
val file = File(application.filesDir, "trap-session.id")
if (!file.exists()) {
    file.writeText(UUID.randomUUID().toString())
}
config.reporter.sessionId = UUID.fromString(file.readText())

// Instantiate the TrapManager
trapManager = TrapManager.getInstance(application, config)
```

### Ask for interactive permissions (if not previously requested) - in both cases
```kotlin
if (!TrapBluetoothCollector.checkPermissions(activity)) {
    TrapBluetoothCollector.requirePermissions(activity) {
    }
}
if (!TrapWiFiCollector.checkPermissions(activity)) {
    TrapWiFiCollector.requirePermissions(activity) {
    }
}
if (!TrapPreciseLocationCollector.checkPermissions(activity)) {
    TrapPreciseLocationCollector.requirePermissions(activity) {
    }
}
```
_Note:_ See the `MainActivity.kt` from the example project for a full example.

## Documentation
Generate documentation from the source comments:
```shell
./gradlew dokkaHtml
```

After which you can find the generated documentation in `build/dokka`

## Dependencies
This library depends on the following 3rd party libraries:
* [Apache Commons Collections 4](https://github.com/apache/commons-collections)
* [Java-WebSocket](https://github.com/TooTallNate/Java-WebSocket)
* [Google Play Services Location](https://mvnrepository.com/artifact/com.google.android.gms/play-services-location?repo=google)

For documentation generation:
* [Dokka](https://github.com/Kotlin/dokka)

## Legal Warning

Many of the data types collected by this library is capable of identifying the individual user, therefore the integrating app can be affected by GDPR and/or CCPA. You are solely responsible for the data collected and processed via this library.

## License

Licensed under the MIT license.