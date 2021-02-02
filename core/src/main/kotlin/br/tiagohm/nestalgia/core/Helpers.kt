package br.tiagohm.nestalgia.core

import java.security.MessageDigest
import java.util.zip.CRC32

fun ByteArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != this[i].toInt()) return false
    return true
}

@ExperimentalUnsignedTypes
fun UByteArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != this[i].toInt()) return false
    return true
}

fun ByteArray.hex(range: IntRange = 0 until size): String {
    val result = ByteArray(size * 2)
    var pos = 0

    fun hexDigit(digit: Int) = if (digit <= 9) (48 + digit).toByte() else (87 + digit).toByte()

    for (i in range) {
        val b = this[i].toInt() and 0xFF
        result[pos++] = hexDigit(b / 16)
        result[pos++] = hexDigit(b % 16)
    }

    return String(result, Charsets.US_ASCII)
}

@ExperimentalUnsignedTypes
fun UByteArray.crc32(range: IntRange = 0 until size): Long {
    val crc32 = CRC32()
    for (i in range) crc32.update(if (i < size) this[i].toInt() else 0)
    return crc32.value
}

@ExperimentalUnsignedTypes
fun UByteArray.md5(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("MD5")
    for (i in range) md.update(if (i < size) this[i].toByte() else 0)
    return md.digest().hex()
}

@ExperimentalUnsignedTypes
fun UByteArray.sha1(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("SHA-1")
    for (i in range) md.update(if (i < size) this[i].toByte() else 0)
    return md.digest().hex()
}

@ExperimentalUnsignedTypes
fun UByteArray.sha256(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("SHA-256")
    for (i in range) md.update(if (i < size) this[i].toByte() else 0)
    return md.digest().hex()
}

fun ByteArray.md5(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("MD5")
    for (i in range) md.update(if (i < size) this[i] else 0)
    return md.digest().hex()
}

fun ByteArray.sha1(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("SHA-1")
    for (i in range) md.update(if (i < size) this[i] else 0)
    return md.digest().hex()
}

fun ByteArray.sha256(range: IntRange = 0 until size): String {
    val md = MessageDigest.getInstance("SHA-256")
    for (i in range) md.update(if (i < size) this[i] else 0)
    return md.digest().hex()
}

fun ByteArray.crc32(range: IntRange = 0 until size): Long {
    val crc32 = CRC32()
    for (i in range) crc32.update(if (i < size) this[i].toInt() else 0)
    return crc32.value
}