package dev.igor.remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import dev.igor.remote.hid.HidController
import dev.igor.remote.hid.MouseButton
import kotlin.math.max
import kotlin.math.min

private const val TAP_TIMEOUT_MS = 220L
private const val DOUBLE_TAP_WINDOW_MS = 300L
private const val SCROLL_DIVISOR_DP = 18f

@Composable
fun TouchpadScreen() {
    var sensitivity by rememberSaveable { mutableFloatStateOf(1.5f) }
    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Touchpad(Modifier.weight(1f).fillMaxWidth(), sensitivity)
        MouseButtons()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Sensibilidade", style = MaterialTheme.typography.bodySmall)
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..4f,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
fun MouseButtons() {
    val haptic = LocalHapticFeedback.current
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                HidController.click(MouseButton.LEFT)
            },
            modifier = Modifier.weight(1f).height(56.dp),
        ) { Text("Esquerdo") }
        FilledTonalButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                HidController.click(MouseButton.RIGHT)
            },
            modifier = Modifier.weight(1f).height(56.dp),
        ) { Text("Direito") }
    }
}

/**
 * Área de touchpad:
 * - 1 dedo arrastando: move o cursor
 * - toque: clique esquerdo; toque com 2 dedos: clique direito
 * - toque + toque-e-arrastar: arrastar (segura o botão esquerdo)
 * - 2 dedos arrastando: rolagem (natural)
 */
@Composable
fun Touchpad(modifier: Modifier = Modifier, sensitivity: Float) {
    val haptic = LocalHapticFeedback.current
    Box(
        modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .pointerInput(sensitivity) {
                var lastTapEnd = 0L
                val scrollDivisor = SCROLL_DIVISOR_DP * density
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val dragging = down.uptimeMillis - lastTapEnd < DOUBLE_TAP_WINDOW_MS
                    if (dragging) HidController.setButton(MouseButton.LEFT, true)

                    var maxPointers = 1
                    var travel = 0f
                    var endTime = down.uptimeMillis
                    while (true) {
                        val event = awaitPointerEvent()
                        val pressed = event.changes.filter { it.pressed }
                        maxPointers = max(maxPointers, pressed.size)
                        endTime = event.changes.maxOf { it.uptimeMillis }

                        if (pressed.size == 1 && maxPointers == 1 || dragging && pressed.size == 1) {
                            val d = pressed[0].positionChange()
                            travel += d.getDistance()
                            val dp = d / density
                            val speed = dp.getDistance()
                            val accel = 1f + min(speed / 12f, 1.5f)
                            HidController.move(dp.x * sensitivity * accel, dp.y * sensitivity * accel)
                        } else if (pressed.size >= 2) {
                            val avg = pressed
                                .map { it.positionChange() }
                                .fold(Offset.Zero) { acc, o -> acc + o } / pressed.size.toFloat()
                            travel += avg.getDistance()
                            HidController.scroll(avg.y / scrollDivisor, -avg.x / scrollDivisor)
                        }
                        event.changes.forEach { it.consume() }
                        if (pressed.isEmpty()) break
                    }

                    val isTap = endTime - down.uptimeMillis < TAP_TIMEOUT_MS &&
                        travel < viewConfiguration.touchSlop
                    when {
                        dragging -> {
                            HidController.setButton(MouseButton.LEFT, false)
                            lastTapEnd = 0L
                        }
                        isTap && maxPointers >= 2 -> {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            HidController.click(MouseButton.RIGHT)
                        }
                        isTap -> {
                            HidController.click(MouseButton.LEFT)
                            lastTapEnd = endTime
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "Arraste para mover · 2 dedos para rolar",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
        )
    }
}
