package dev.igor.remote.hid

import org.junit.Assert.assertEquals
import org.junit.Test

class MouseMathTest {
    @Test fun clamp() {
        assertEquals(127, MouseMath.clamp(500))
        assertEquals(-127, MouseMath.clamp(-500))
        assertEquals(3, MouseMath.clamp(3))
    }
}
