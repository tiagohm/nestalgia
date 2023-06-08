package br.tiagohm.nestalgia.desktop

import java.util.*

fun base64Encode(text: String): String {
    return Base64.getEncoder().encodeToString(text.toByteArray())
}

fun base64Decode(text: String): String {
    return String(Base64.getDecoder().decode(text.toByteArray()))
}
