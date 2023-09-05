package com.cursorinsight.trap.example

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cursorinsight.trap.datasource.TrapBluetoothCollector
import com.cursorinsight.trap.datasource.TrapPreciseLocationCollector
import com.cursorinsight.trap.datasource.TrapWiFiCollector
import com.cursorinsight.trap.example.ui.theme.ExampleTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ExampleTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Example(this)
                }
            }
        }
    }

}

@Composable
fun Example(activity: Activity) {
    Box(contentAlignment = Alignment.Center) {
        Column {
            Text(text = "Hello world!")
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Button(onClick = {
                    if (!TrapBluetoothCollector.checkPermissions(activity)) {
                        TrapBluetoothCollector.requirePermissions(activity) {
                        }
                    }
                }) {
                    Text(text = "Request Bluetooth permission")
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Button(onClick = {
                    if (!TrapWiFiCollector.checkPermissions(activity)) {
                        TrapWiFiCollector.requirePermissions(activity) {
                        }
                    }
                }) {
                    Text(text = "Request WiFi permission")
                }
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .padding(10.dp)
            ) {
                Button(onClick = {
                    if (!TrapPreciseLocationCollector.checkPermissions(activity)) {
                        TrapPreciseLocationCollector.requirePermissions(activity) {
                        }
                    }
                }) {
                    Text(text = "Request Location permission")
                }
            }
        }
    }
}
