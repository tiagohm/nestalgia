package br.tiagohm.nestalgia.core

fun ByteArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != this[i].toInt()) return false
    return true
}

@ExperimentalUnsignedTypes
fun UByteArray.startsWith(text: String): Boolean {
    for (i in text.indices) if (text.codePointAt(i) != this[i].toInt()) return false
    return true
}