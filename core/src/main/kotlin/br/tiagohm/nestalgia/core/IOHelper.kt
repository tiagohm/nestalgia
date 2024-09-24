@file:Suppress("NOTHING_TO_INLINE")

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

fun ByteArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != (this[i].toInt() and 0xFF)) return false
    return true
}

fun ByteArray.endsWith(text: String): Boolean {
    val idx = size - text.length
    for (i in text.indices) if (text.codePointAt(i) != (this[idx + i].toInt() and 0xFF)) return false
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

fun ByteArray.crc32(range: IntRange = indices): Long {
    val hash = CRC32()
    for (i in range) hash.update(this[i].toInt())
    return hash.value
}

fun IntArray.digest(algorithm: String, range: IntRange = indices): String {
    val hash = MessageDigest.getInstance(algorithm)
    for (i in range) hash.update(this[i].toByte())
    return hash.digest().hex()
}

fun ByteArray.digest(algorithm: String, range: IntRange = indices): String {
    val hash = MessageDigest.getInstance(algorithm)
    for (i in range) hash.update(this[i])
    return hash.digest().hex()
}

@Suppress("NOTHING_TO_INLINE")
inline fun IntArray.md5(range: IntRange = indices): String {
    return digest("MD5", range)
}

inline fun ByteArray.md5(range: IntRange = indices): String {
    return digest("MD5", range)
}

inline fun IntArray.sha1(range: IntRange = indices): String {
    return digest("SHA-1", range)
}

inline fun ByteArray.sha1(range: IntRange = indices): String {
    return digest("SHA-1", range)
}

inline fun IntArray.sha256(range: IntRange = indices): String {
    return digest("SHA-256", range)
}

inline fun ByteArray.sha256(range: IntRange = indices): String {
    return digest("SHA-256", range)
}

inline fun ByteArray.toIntArray(size: Int = this.size): IntArray {
    return IntArray(size) { this[it].toUnsignedInt() }
}

inline fun Byte.toUnsignedInt(): Int {
    return toInt() and 0xFF
}
