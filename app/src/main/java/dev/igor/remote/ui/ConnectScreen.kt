package dev.igor.remote.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.igor.remote.hid.HidController
import dev.igor.remote.hid.HidState
import dev.igor.remote.service.safeName

@SuppressLint("MissingPermission")
@Composable
fun ConnectScreen(permitted: Boolean, onRequestPermissions: () -> Unit) {
    val context = LocalContext.current
    val state by HidController.state.collectAsStateWithLifecycle()
    var devices by remember { mutableStateOf(emptyList<BluetoothDevice>()) }

    val discoverable = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { devices = HidController.bondedDevices() }

    LaunchedEffect(state, permitted) {
        if (permitted) devices = HidController.bondedDevices()
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        StatusCard(state)

        if (!permitted) {
            Text("O app precisa da permissão de Bluetooth para funcionar como teclado e mouse.")
            Button(onClick = onRequestPermissions) { Text("Conceder permissões") }
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                discoverable.launch(
                    Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
                        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120),
                )
            }) { Text("Tornar visível") }

            when (state) {
                is HidState.Connected -> OutlinedButton(onClick = HidController::disconnect) {
                    Text("Desconectar")
                }
                is HidState.Unavailable, HidState.Idle -> OutlinedButton(
                    onClick = { HidController.start(context) },
                ) { Text("Tentar novamente") }
                else -> Unit
            }
        }

        Text(
            "Primeira vez: toque em “Tornar visível” e pareie pelo notebook " +
                "(configurações de Bluetooth ou `bluetoothctl`). Depois, basta tocar no notebook abaixo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text("Dispositivos pareados", style = MaterialTheme.typography.titleMedium)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(devices, key = { it.address }) { device ->
                val connectedHere = (state as? HidState.Connected)?.device == device
                ListItem(
                    headlineContent = { Text(device.safeName()) },
                    supportingContent = { Text(device.address) },
                    leadingContent = { Icon(Icons.Filled.Computer, null) },
                    trailingContent = {
                        if (connectedHere) {
                            Text("Conectado", color = MaterialTheme.colorScheme.primary)
                        } else {
                            TextButton(
                                enabled = state !is HidState.Unavailable && state != HidState.Idle,
                                onClick = { HidController.connect(device) },
                            ) { Text("Conectar") }
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                )
            }
        }
    }
}

@Composable
private fun StatusCard(state: HidState) {
    val (title, subtitle) = when (state) {
        is HidState.Connected -> "Conectado" to state.device.safeName()
        is HidState.Connecting -> "Conectando…" to state.device.safeName()
        HidState.Registered -> "Pronto" to "Aguardando o notebook conectar"
        HidState.Idle -> "Desligado" to "Perfil HID não registrado"
        is HidState.Unavailable -> "Indisponível" to state.reason
    }
    val container = if (state is HidState.Connected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(Modifier.padding(20.dp)) {
            Column {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.width(4.dp))
                Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
