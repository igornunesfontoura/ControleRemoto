package dev.igor.remote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.igor.remote.hid.HidController
import dev.igor.remote.hid.Key
import dev.igor.remote.hid.Modifier as Mod

/**
 * O campo mantém sempre um texto-sentinela; qualquer diferença em relação a ele
 * vira teclas enviadas (caracteres novos) ou Backspace (caracteres apagados).
 * Assim o Backspace funciona mesmo com o "campo vazio".
 */
private const val SENTINEL = "  "

private fun sentinelValue() = TextFieldValue(SENTINEL, TextRange(SENTINEL.length))

@Composable
fun KeyboardScreen() {
    var value by remember { mutableStateOf(sentinelValue()) }
    val focus = remember { FocusRequester() }
    val sticky by HidController.stickyModifiers.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { focus.requestFocus() }

    Column(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = { new ->
                val old = value.text
                val prefix = old.commonPrefixWith(new.text).length
                repeat(old.length - prefix) { HidController.pressKey(Key.BACKSPACE) }
                new.text.substring(prefix).forEach(HidController::type)
                value = sentinelValue()
            },
            modifier = Modifier.fillMaxWidth().focusRequester(focus),
            label = { Text("Toque aqui e digite") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Send,
            ),
            keyboardActions = KeyboardActions(onSend = { HidController.pressKey(Key.ENTER) }),
        )
        Text(
            "Layout do notebook assumido: US. Acentos são enviados sem acento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ModifierChip("Ctrl", Mod.CTRL, sticky)
            ModifierChip("Alt", Mod.ALT, sticky)
            ModifierChip("Shift", Mod.SHIFT, sticky)
            ModifierChip("Super", Mod.SUPER, sticky)
        }
        KeyRow {
            KeyButton("Esc", Key.ESCAPE)
            KeyButton("Tab", Key.TAB)
            KeyButton("↑", Key.UP)
            KeyButton("⌫", Key.BACKSPACE)
        }
        KeyRow {
            KeyButton("←", Key.LEFT)
            KeyButton("↓", Key.DOWN)
            KeyButton("→", Key.RIGHT)
            KeyButton("Enter", Key.ENTER)
        }
        KeyRow {
            KeyButton("Home", Key.HOME)
            KeyButton("End", Key.END)
            KeyButton("PgUp", Key.PAGE_UP)
            KeyButton("PgDn", Key.PAGE_DOWN)
        }
        KeyRow {
            KeyButton("Espaço", Key.SPACE)
            KeyButton("Del", Key.DELETE)
            KeyButton("F5", Key.F5)
            KeyButton("F11", Key.F11)
        }
    }
}

@Composable
private fun KeyRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RowScope.KeyButton(label: String, code: Int) {
    FilledTonalButton(
        onClick = { HidController.pressKey(code) },
        modifier = Modifier.weight(1f).height(52.dp),
    ) { Text(label, maxLines = 1) }
}

@Composable
private fun RowScope.ModifierChip(label: String, bit: Int, sticky: Int) {
    FilterChip(
        selected = sticky and bit != 0,
        onClick = { HidController.toggleSticky(bit) },
        label = { Text(label) },
        modifier = Modifier.weight(1f),
    )
}
