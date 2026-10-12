package dev.igor.remote.hid

/** Uma tecla HID com seus modificadores. */
data class KeyStroke(val modifiers: Int, val code: Int)

/** Bits de modificador do report de teclado. */
object Modifier {
    const val NONE = 0x00
    const val CTRL = 0x01
    const val SHIFT = 0x02
    const val ALT = 0x04
    const val SUPER = 0x08
}

/** Usage codes de teclas especiais (página 0x07). */
object Key {
    const val ENTER = 0x28
    const val ESCAPE = 0x29
    const val BACKSPACE = 0x2A
    const val TAB = 0x2B
    const val SPACE = 0x2C
    const val F = 0x09
    const val F5 = 0x3E
    const val F11 = 0x44
    const val HOME = 0x4A
    const val PAGE_UP = 0x4B
    const val DELETE = 0x4C
    const val END = 0x4D
    const val PAGE_DOWN = 0x4E
    const val RIGHT = 0x4F
    const val LEFT = 0x50
    const val DOWN = 0x51
    const val UP = 0x52
}

/**
 * Converte caracteres em teclas HID assumindo que o notebook usa layout **US**.
 * Caracteres acentuados são decompostos e enviados sem acento (melhor esforço).
 */
object KeyMapper {
    private val map: Map<Char, KeyStroke> = buildMap {
        for ((i, c) in ('a'..'z').withIndex()) {
            put(c, KeyStroke(Modifier.NONE, 0x04 + i))
            put(c.uppercaseChar(), KeyStroke(Modifier.SHIFT, 0x04 + i))
        }
        "1234567890".forEachIndexed { i, c -> put(c, KeyStroke(Modifier.NONE, 0x1E + i)) }
        "!@#$%^&*()".forEachIndexed { i, c -> put(c, KeyStroke(Modifier.SHIFT, 0x1E + i)) }

        put('\n', KeyStroke(Modifier.NONE, Key.ENTER))
        put('\t', KeyStroke(Modifier.NONE, Key.TAB))
        put(' ', KeyStroke(Modifier.NONE, Key.SPACE))

        val pairs = listOf(
            Triple('-', '_', 0x2D), Triple('=', '+', 0x2E), Triple('[', '{', 0x2F),
            Triple(']', '}', 0x30), Triple('\\', '|', 0x31), Triple(';', ':', 0x33),
            Triple('\'', '"', 0x34), Triple('`', '~', 0x35), Triple(',', '<', 0x36),
            Triple('.', '>', 0x37), Triple('/', '?', 0x38),
        )
        for ((plain, shifted, code) in pairs) {
            put(plain, KeyStroke(Modifier.NONE, code))
            put(shifted, KeyStroke(Modifier.SHIFT, code))
        }
    }

    fun map(c: Char): KeyStroke? = map[c] ?: map[stripAccent(c)]

    private fun stripAccent(c: Char): Char {
        val decomposed = java.text.Normalizer.normalize(c.toString(), java.text.Normalizer.Form.NFD)
        return decomposed.firstOrNull() ?: c
    }
}
