package com.vbwd.plugin.meinchatplus.domain

import java.io.ByteArrayOutputStream

/** Wire-format envelope for an `e2e_v1` ciphertext (one slot per device). */
data class Envelope(val v: Int = 1, val perRecipient: List<Slot>) {
    data class Slot(val deviceId: String, val header: ByteArray, val ciphertext: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Slot) return false
            return deviceId == other.deviceId &&
                header.contentEquals(other.header) &&
                ciphertext.contentEquals(other.ciphertext)
        }
        override fun hashCode(): Int =
            (deviceId.hashCode() * HASH_PRIME + header.contentHashCode()) * HASH_PRIME + ciphertext.contentHashCode()

        private companion object {
            const val HASH_PRIME = 31
        }
    }
}

/**
 * Packs/unpacks [Envelope] to/from a restricted CBOR grammar shared with the
 * server (`cbor2`) + web (`cbor-x`). Definite-length maps/arrays, UTF-8 text
 * keys, major types 0/2/3/4/5 — no tags, no floats. Port of the iOS
 * `EnvelopePacker`. The numeric constants below are the CBOR wire format.
 */
@Suppress("MagicNumber")
object EnvelopePacker {
    sealed class EnvelopeError(message: String) : Exception(message) {
        data object Truncated : EnvelopeError("truncated input")
        data object WrongType : EnvelopeError("unexpected CBOR major type")
        data object KeyNotText : EnvelopeError("map key is not text")
        data object LengthExceedsBuffer : EnvelopeError("length exceeds buffer")
        data class MissingKey(val key: String) : EnvelopeError("missing key: $key")
        data class UnsupportedLengthEncoding(val info: Int) : EnvelopeError("bad length encoding: $info")
    }

    fun pack(envelope: Envelope): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(typed(major = 5, value = 2))
        out.write(cborText("v"))
        out.write(typed(major = 0, value = envelope.v.toLong()))
        out.write(cborText("perRecipient"))
        out.write(typed(major = 4, value = envelope.perRecipient.size.toLong()))
        for (slot in envelope.perRecipient) {
            out.write(typed(major = 5, value = 3))
            out.write(cborText("device_id"))
            out.write(cborText(slot.deviceId))
            out.write(cborText("header"))
            out.write(cborBytes(slot.header))
            out.write(cborText("ciphertext"))
            out.write(cborBytes(slot.ciphertext))
        }
        return out.toByteArray()
    }

    fun unpack(data: ByteArray): Envelope {
        val cursor = Cursor(data)
        val top = cursor.readMap()
        var version: Int? = null
        val slots = mutableListOf<Envelope.Slot>()
        repeat(top.toInt()) {
            when (cursor.readText()) {
                "v" -> version = cursor.readUInt().toInt()
                "perRecipient" -> repeat(cursor.readArray().toInt()) { slots.add(readSlot(cursor)) }
                else -> cursor.skipOneValue()
            }
        }
        return Envelope(v = version ?: throw EnvelopeError.MissingKey("v"), perRecipient = slots)
    }

    private fun readSlot(cursor: Cursor): Envelope.Slot {
        val count = cursor.readMap()
        var deviceId: String? = null
        var header: ByteArray? = null
        var ciphertext: ByteArray? = null
        repeat(count.toInt()) {
            when (cursor.readText()) {
                "device_id" -> deviceId = cursor.readText()
                "header" -> header = cursor.readBytes()
                "ciphertext" -> ciphertext = cursor.readBytes()
                else -> cursor.skipOneValue()
            }
        }
        return Envelope.Slot(
            deviceId = deviceId ?: throw EnvelopeError.MissingKey("device_id"),
            header = header ?: throw EnvelopeError.MissingKey("header"),
            ciphertext = ciphertext ?: throw EnvelopeError.MissingKey("ciphertext"),
        )
    }

    private fun cborText(s: String): ByteArray {
        val bytes = s.toByteArray(Charsets.UTF_8)
        return typed(major = 3, value = bytes.size.toLong()) + bytes
    }

    private fun cborBytes(d: ByteArray): ByteArray = typed(major = 2, value = d.size.toLong()) + d

    private fun typed(major: Int, value: Long): ByteArray {
        val prefix = (major shl 5)
        return when {
            value < 24 -> byteArrayOf((prefix or value.toInt()).toByte())
            value <= 0xFF -> byteArrayOf((prefix or 24).toByte(), value.toByte())
            value <= 0xFFFF -> byteArrayOf(
                (prefix or 25).toByte(),
                (value shr 8).toByte(),
                value.toByte(),
            )
            value <= 0xFFFFFFFFL -> byteArrayOf(
                (prefix or 26).toByte(),
                (value shr 24).toByte(),
                (value shr 16).toByte(),
                (value shr 8).toByte(),
                value.toByte(),
            )
            else -> ByteArray(9).also { buf ->
                buf[0] = (prefix or 27).toByte()
                for (i in 0 until 8) buf[8 - i] = (value shr (i * 8)).toByte()
            }
        }
    }

    @Suppress("MagicNumber")
    private class Cursor(private val data: ByteArray) {
        private var index = 0

        private fun readByte(): Int {
            if (index >= data.size) throw EnvelopeError.Truncated
            return data[index++].toInt() and 0xFF
        }

        private fun readRaw(count: Int): ByteArray {
            if (count < 0 || index + count > data.size) throw EnvelopeError.LengthExceedsBuffer
            return data.copyOfRange(index, index + count).also { index += count }
        }

        private fun readHeader(): Pair<Int, Long> {
            val b = readByte()
            val major = b shr 5
            return when (val additional = b and 0x1F) {
                in 0..23 -> major to additional.toLong()
                24 -> major to readByte().toLong()
                25 -> major to ((readByte().toLong() shl 8) or readByte().toLong())
                26 -> {
                    var v = 0L
                    repeat(4) { v = (v shl 8) or readByte().toLong() }
                    major to v
                }
                27 -> {
                    var v = 0L
                    repeat(8) { v = (v shl 8) or readByte().toLong() }
                    major to v
                }
                else -> throw EnvelopeError.UnsupportedLengthEncoding(additional)
            }
        }

        fun readMap(): Long = expect(5)
        fun readArray(): Long = expect(4)
        fun readUInt(): Long = expect(0)
        fun readBytes(): ByteArray = readRaw(expect(2).toInt())
        fun readText(): String {
            val (major, count) = readHeader()
            if (major != 3) throw EnvelopeError.KeyNotText
            return String(readRaw(count.toInt()), Charsets.UTF_8)
        }

        fun skipOneValue() {
            val (major, count) = readHeader()
            when (major) {
                0, 1 -> Unit
                2, 3 -> readRaw(count.toInt())
                4 -> repeat(count.toInt()) { skipOneValue() }
                5 -> repeat(count.toInt() * 2) { skipOneValue() }
                else -> throw EnvelopeError.WrongType
            }
        }

        private fun expect(major: Int): Long {
            val (m, c) = readHeader()
            if (m != major) throw EnvelopeError.WrongType
            return c
        }
    }
}
