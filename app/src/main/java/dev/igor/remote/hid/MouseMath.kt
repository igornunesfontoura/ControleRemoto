package dev.igor.remote.hid

object MouseMath {
    fun clamp(v: Int): Int = v.coerceIn(-127, 127)
}
