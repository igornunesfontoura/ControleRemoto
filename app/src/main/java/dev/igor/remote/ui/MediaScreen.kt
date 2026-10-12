package dev.igor.remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay5
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Forward5
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.igor.remote.hid.Consumer
import dev.igor.remote.hid.HidController
import dev.igor.remote.hid.Key

@Composable
fun MediaScreen() {
    val haptic = LocalHapticFeedback.current
    fun media(usage: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        HidController.media(usage)
    }
    fun key(code: Int) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        HidController.pressKey(code)
    }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            MediaButton(Icons.Filled.SkipPrevious, "Anterior") { media(Consumer.PREVIOUS) }
            FilledIconButton(
                onClick = { media(Consumer.PLAY_PAUSE) },
                modifier = Modifier.size(96.dp),
            ) { Icon(Icons.Filled.PlayArrow, "Play/Pause", Modifier.size(56.dp)) }
            MediaButton(Icons.Filled.SkipNext, "Próximo") { media(Consumer.NEXT) }
        }
        ControlRow {
            MediaButton(Icons.Filled.VolumeDown, "Volume −", Modifier.weight(1f)) { media(Consumer.VOLUME_DOWN) }
            MediaButton(Icons.AutoMirrored.Filled.VolumeOff, "Mudo", Modifier.weight(1f)) { media(Consumer.MUTE) }
            MediaButton(Icons.AutoMirrored.Filled.VolumeUp, "Volume +", Modifier.weight(1f)) { media(Consumer.VOLUME_UP) }
        }
        ControlRow {
            MediaButton(Icons.Filled.Replay5, "Voltar (←)", Modifier.weight(1f)) { key(Key.LEFT) }
            MediaButton(Icons.Filled.Fullscreen, "Tela cheia (F)", Modifier.weight(1f)) { key(Key.F) }
            MediaButton(Icons.Filled.Forward5, "Avançar (→)", Modifier.weight(1f)) { key(Key.RIGHT) }
        }
        Text(
            "Play/Pause e volume usam teclas de mídia; ←/→ e F funcionam no YouTube, mpv e VLC.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Touchpad(Modifier.weight(1f).fillMaxWidth(), sensitivity = 1.5f)
        MouseButtons()
    }
}

@Composable
private fun ControlRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content,
    )
}

@Composable
private fun MediaButton(
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier.width(64.dp),
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = modifier.height(64.dp),
        colors = IconButtonDefaults.filledTonalIconButtonColors(),
    ) { Icon(icon, description, Modifier.size(32.dp)) }
}
