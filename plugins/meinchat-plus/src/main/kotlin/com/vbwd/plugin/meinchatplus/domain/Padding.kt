package com.vbwd.plugin.meinchatplus.domain

import java.nio.ByteBuffer
import java.security.SecureRandom

/**
 * 256-byte-multiple plaintext padding (S28.7 §3.2). An observer of the
 * ciphertext learns the original length only to within ± 256 bytes.
 *
 * Layout: `[ 4-byte big-endian length ][ payload ][ random tail ]`, total rounded
 * up to the next multiple of 256. Port of the iOS `Padding`.
 */
object Padding {
    const val BLOCK_SIZE = 256
    private const val HEADER_SIZE = 4

    sealed class PaddingError(message: String) : Exception(message) {
        data object TruncatedHeader : PaddingError("padded buffer is shorter than its header")
        data object LengthExceedsBuffer : PaddingError("declared length exceeds the buffer")
    }

    fun padTo256(plaintext: String): ByteArray = pad(plaintext.toByteArray(Charsets.UTF_8))

    fun pad(payload: ByteArray): ByteArray {
        val header = ByteBuffer.allocate(HEADER_SIZE).putInt(payload.size).array()
        val body = header + payload
        val target = nextBlockBoundary(body.size)
        return body + randomBytes(target - body.size)
    }

    fun strip(padded: ByteArray): String = String(stripToBytes(padded), Charsets.UTF_8)

    fun stripToBytes(padded: ByteArray): ByteArray {
        if (padded.size < HEADER_SIZE) throw PaddingError.TruncatedHeader
        val length = ByteBuffer.wrap(padded, 0, HEADER_SIZE).int
        val end = HEADER_SIZE + length
        if (length < 0 || end > padded.size) throw PaddingError.LengthExceedsBuffer
        return padded.copyOfRange(HEADER_SIZE, end)
    }

    private fun nextBlockBoundary(size: Int): Int {
        val remainder = size % BLOCK_SIZE
        return if (remainder == 0) maxOf(size, BLOCK_SIZE) else size + (BLOCK_SIZE - remainder)
    }

    private fun randomBytes(count: Int): ByteArray {
        if (count <= 0) return ByteArray(0)
        return ByteArray(count).also { SecureRandom().nextBytes(it) }
    }
}
