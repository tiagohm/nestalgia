package br.tiagohm.nestalgia.core

import java.security.MessageDigest
import java.util.zip.CRC32

fun IntArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != this[i]) return false
    return true
}

fun IntArray.endsWith(text: String): Boolean {
    val idx = size - text.length
    for (i in text.indices) if (text.codePointAt(i) != this[idx + i]) return false
    return true
}

fun ByteArray.hex(range: IntRange = indices, digits: String = "0123456789abcdef"): String {
    val result = CharArray(size * 2)
    var c = 0

    for (i in range) {
        val value = this[i].toInt()
        result[c++] = digits[value shr 4 and 0xF]
        result[c++] = digits[value and 0xF]
    }

    return result.concatToString()
}

fun IntArray.crc32(range: IntRange = indices): Long {
    val hash = CRC32()
    for (i in range) hash.update(this[i])
    return hash.value
}

fun IntArray.digest(algorithm: String, range: IntRange = indices): String {
    val hash = MessageDigest.getInstance(algorithm)
    for (i in range) hash.update(this[i].toByte())
    return hash.digest().hex()
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntArray.md5(range: IntRange = indices): String {
    return digest("MD5", range)
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntArray.sha1(range: IntRange = indices): String {
    return digest("SHA-1", range)
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntArray.sha256(range: IntRange = indices): String {
    return digest("SHA-256", range)
}

@Suppress("NOTHING_TO_INLINE")
inline fun ByteArray.toIntArray(size: Int = this.size): IntArray {
    return IntArray(size) { this[it].toInt() and 0xFF }
}
