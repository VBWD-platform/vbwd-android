package com.vbwd.plugin.meinchatplus.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EnvelopePackerTest {
    @Test
    fun `pack then unpack round-trips a multi-slot envelope`() {
        val envelope = Envelope(
            v = 1,
            perRecipient = listOf(
                Envelope.Slot("d1", byteArrayOf(1, 2, 3), byteArrayOf(9, 8, 7)),
                // a long header exercises the multi-byte CBOR length encoding
                Envelope.Slot("d2", ByteArray(300) { it.toByte() }, byteArrayOf(0)),
            ),
        )
        val unpacked = EnvelopePacker.unpack(EnvelopePacker.pack(envelope))
        assertEquals(envelope, unpacked)
    }

    @Test
    fun `unknown top-level keys are skipped (forward compat)`() {
        val envelope = Envelope(v = 2, perRecipient = listOf(Envelope.Slot("d", byteArrayOf(1), byteArrayOf(2))))
        val unpacked = EnvelopePacker.unpack(EnvelopePacker.pack(envelope))
        assertEquals(2, unpacked.v)
        assertEquals("d", unpacked.perRecipient.single().deviceId)
    }
}
