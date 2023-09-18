package com.cursorinsight.trap.datasource

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.mockkObject
import io.mockk.mockkStatic
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class TrapBluetoothCollectorTest {
    @Test
    fun `test Bluetooth event`() {

    }

    @Test
    fun `test permission check`(@MockK activity: Activity) {
        mockkStatic(ContextCompat::class)
        every { ContextCompat.checkSelfPermission(any(), any()) } returns PackageManager.PERMISSION_GRANTED

        val result = TrapBluetoothCollector.checkPermissions(activity)
        assert(result)
    }

    @Test
    fun `test require permission`() {
        val succeeded = mockkStatic({})

        TrapBluetoothCollector.requirePermissions()
    }
}