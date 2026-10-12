package dev.igor.remote

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mouse
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import dev.igor.remote.service.HidService
import dev.igor.remote.ui.ConnectScreen
import dev.igor.remote.ui.KeyboardScreen
import dev.igor.remote.ui.MediaScreen
import dev.igor.remote.ui.TouchpadScreen
import dev.igor.remote.ui.theme.RemoteTheme

private enum class Tab(val label: String, val icon: ImageVector) {
    Connect("Conectar", Icons.Filled.Bluetooth),
    Touchpad("Touchpad", Icons.Filled.Mouse),
    Keyboard("Teclado", Icons.Filled.Keyboard),
    Media("Mídia", Icons.Filled.PlayCircle),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RemoteTheme { App() }
        }
    }

    private fun requiredPermissions(): Array<String> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            add(Manifest.permission.BLUETOOTH_CONNECT)
            add(Manifest.permission.BLUETOOTH_ADVERTISE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private fun hasBluetoothPermission(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED

    @Composable
    private fun App() {
        var tab by rememberSaveable { mutableIntStateOf(Tab.Connect.ordinal) }
        var permitted by remember { mutableStateOf(hasBluetoothPermission()) }

        val launcher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions(),
        ) {
            permitted = hasBluetoothPermission()
        }

        LaunchedEffect(permitted) {
            if (permitted) HidService.start(this@MainActivity)
            else launcher.launch(requiredPermissions())
        }

        Scaffold(
            bottomBar = {
                NavigationBar {
                    Tab.entries.forEach { t ->
                        NavigationBarItem(
                            selected = tab == t.ordinal,
                            onClick = { tab = t.ordinal },
                            icon = { Icon(t.icon, contentDescription = null) },
                            label = { Text(t.label) },
                        )
                    }
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                when (Tab.entries[tab]) {
                    Tab.Connect -> ConnectScreen(
                        permitted = permitted,
                        onRequestPermissions = { launcher.launch(requiredPermissions()) },
                    )
                    Tab.Touchpad -> TouchpadScreen()
                    Tab.Keyboard -> KeyboardScreen()
                    Tab.Media -> MediaScreen()
                }
            }
        }
    }
}
