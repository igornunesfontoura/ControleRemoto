package dev.igor.remote.hid

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class KeyMapperTest {
    @Test fun lowercaseLetters() = assertEquals(KeyStroke(Modifier.NONE, 0x04), KeyMapper.map('a'))
    @Test fun uppercaseUsesShift() = assertEquals(KeyStroke(Modifier.SHIFT, 0x1D), KeyMapper.map('Z'))
    @Test fun digits() {
        assertEquals(KeyStroke(Modifier.NONE, 0x1E), KeyMapper.map('1'))
        assertEquals(KeyStroke(Modifier.NONE, 0x27), KeyMapper.map('0'))
    }
    @Test fun shiftedSymbols() {
        assertEquals(KeyStroke(Modifier.SHIFT, 0x1F), KeyMapper.map('@'))
        assertEquals(KeyStroke(Modifier.SHIFT, 0x38), KeyMapper.map('?'))
    }
    @Test fun whitespace() {
        assertEquals(KeyStroke(Modifier.NONE, Key.SPACE), KeyMapper.map(' '))
        assertEquals(KeyStroke(Modifier.NONE, Key.ENTER), KeyMapper.map('\n'))
    }
    @Test fun accentsAreStripped() {
        assertEquals(KeyMapper.map('a'), KeyMapper.map('ã'))
        assertEquals(KeyMapper.map('C'), KeyMapper.map('Ç'))
    }
    @Test fun unknownCharIsNull() = assertNull(KeyMapper.map('€'))
}
