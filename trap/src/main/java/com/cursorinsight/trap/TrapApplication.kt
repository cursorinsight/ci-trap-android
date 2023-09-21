package com.cursorinsight.trap

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import java.io.File
import java.lang.ref.WeakReference
import java.util.UUID

/**
 * The Application class you can use in the AndroidManifest.xml
 * <application> tag with the 'android:name' parameter. This
 * custom Application class initializes and manages the data
 * collection automatically.
 *
 * You can set a custom configuration by implementing TrapConfigProvider
 * and setting your class as "trap:config" metadata on the <application>
 * tag.
 *
 * ```xml
 *   <application
 *     "android:name" = "com.cursorinsight.trap.TrapApplication"
 *     ...
 *   >
 *     <meta-data android:name="trap:config" android:value="com.company.MyConfig" />
 *   </application>
 * ```
 */
open class TrapApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initialize(this)
    }

    companion object {
        /**
         * A weak reference to the global Application class.
         */
        private var application: WeakReference<Application> = WeakReference(null)

        /**
         * The singleton instance of the TrapManager class.
         */
        private lateinit var trapManager: TrapManager

        /**
         * Start the Trap system for the given application.
         *
         * Note: The library cannot be reinitialized by calling
         * this method with a different Application instance.
         *
         * @param application The global android Application instance.
         * @return The singletonTrapManager instance.
         */
        fun initialize(application: Application): TrapManager {
            if (TrapApplication.application.get() == null) {
                TrapApplication.application = WeakReference(application)
                trapManager = TrapManager.getInstance(application, getConfig(application))
            }

            return trapManager
        }

        /**
         * Get the configuration (customized or default) instance.
         *
         * @param application The global android Application instance.
         * @return The configuration, potentially modified for your requirements.
         */
        private fun getConfig(application: Application): TrapConfig {
            val info = getMetadata()?.getString("trap:config") ?: return with(TrapConfig()) {
                val file = File(application.filesDir, "trap-session.id")
                if (!file.exists()) {
                    file.writeText(UUID.randomUUID().toString())
                }
                reporter.sessionId = UUID.fromString(file.readText())
                this
            }
            val provider = try {
                val instance = Class.forName(info).newInstance() as? TrapConfigProvider
                if (instance == null) {
                    Log.e(
                        TrapApplication::class.simpleName,
                        "The instance of the provided android:trap class is not an TrapConfigProvider"
                    )
                }
                instance
            } catch (_: ClassNotFoundException) {
                Log.e(
                    TrapApplication::class.simpleName,
                    "The provided android:trap parameter class name is not found"
                )
                null
            } catch (_: InstantiationException) {
                Log.e(
                    TrapApplication::class.simpleName,
                    "The provided android:trap class cannot be instantiated or doesn't have a simple constructor"
                )
                null
            } catch (_: IllegalAccessException) {
                Log.e(
                    TrapApplication::class.simpleName,
                    "The provided android:trap class cannot be accessed"
                )
                null
            }

            return provider?.getConfig(application) ?: TrapConfig()
        }

        /**
         * Get the AndroidManifest.xml application meta-data Bundle.
         *
         * @return The meta-data from the AndroidManifest.xml
         */
        private fun getMetadata(): Bundle? {
            val packageName: String = application.get()?.packageName ?: return null

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                application.get()?.packageManager?.getPackageInfo(
                    packageName, PackageManager.PackageInfoFlags.of(
                        PackageManager.GET_META_DATA.toLong()
                    )
                )
            } else {
                @Suppress("DEPRECATION") application.get()?.packageManager?.getPackageInfo(
                    packageName,
                    PackageManager.GET_META_DATA
                )
            }?.applicationInfo?.metaData
        }
    }
}
