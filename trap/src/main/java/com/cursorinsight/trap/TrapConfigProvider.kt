package com.cursorinsight.trap

import android.app.Application

/**
 * Implement this interface and set the class in the
 * ApplicationManifest.xml under <application> as <meta-data>
 * to provide your own custom configuration for the Trap platform.
 */
interface TrapConfigProvider {
    /**
     * Returns a config instance to use with the Trap library.
     *
     * @param application The global Android application instance.
     * @return The configuration, potentially modified for your requirements.
     */
    fun getConfig(application: Application): TrapConfig
}