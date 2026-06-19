package com.vbwd.plugin.meinchatplus.domain

import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PaddingTest {
    @Test
    fun `padded length is always a multiple of the block size`() {
        assertEquals(0, Padding.pad("hi".toByteArray()).size % Padding.BLOCK_SIZE)
        assertEquals(0, Padding.pad(ByteArray(300)).size % Padding.BLOCK_SIZE)
    }

    @Test
    fun `a 300-byte payload rounds up to 512 (4-byte header + payload)`() {
        assertEquals(512, Padding.pad(ByteArray(300)).size)
    }

    @Test
    fun `pad then strip round-trips the original bytes and string`() {
        val payload = ByteArray(300) { it.toByte() }
        assertArrayEquals(payload, Padding.stripToBytes(Padding.pad(payload)))
        assertEquals("héllo wörld", Padding.strip(Padding.padTo256("héllo wörld")))
    }

    @Test
    fun `a too-short buffer raises TruncatedHeader`() {
        val error = runCatching { Padding.stripToBytes(ByteArray(2)) }.exceptionOrNull()
        assertTrue(error is Padding.PaddingError.TruncatedHeader)
    }
}
