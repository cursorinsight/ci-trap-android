package com.cursorinsight.trap

import android.app.Application
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import io.mockk.mockkClass
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.io.File

@ExtendWith(MockKExtension::class)
class TrapApplicationTest {
    private val tempDir = System.getProperty("java.io.tmpdir")

    private lateinit var application: Application

    @BeforeEach
    fun setUp() {
        application = mockkClass(Application::class)
        every { application.packageName } returns "com.cursorinsight.trap"
        every { application.registerActivityLifecycleCallbacks(any()) } returns Unit
        every { application.packageManager } answers {
            val packageManager = mockkClass(PackageManager::class)

            every { packageManager.getPackageInfo(any(String::class), any(Int::class)) } answers {
                val pInfo = mockkClass(PackageInfo::class)
                pInfo.applicationInfo = mockkClass(ApplicationInfo::class)
                pInfo.applicationInfo.metaData = mockkClass(Bundle::class)

                every { pInfo.applicationInfo.metaData.getString(any()) } returns null

                pInfo
            }

            packageManager
        }
        every { application.filesDir } returns File(tempDir)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test manager creation`() {
        val manager = TrapApplication.initialize(application)
        assert(manager != null)
    }

    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            mockkStatic(Log::class)
            every { Log.v(any(), any()) } returns 0
            every { Log.d(any(), any()) } returns 0
            every { Log.i(any(), any()) } returns 0
            every { Log.e(any(), any()) } returns 0
        }
    }
}