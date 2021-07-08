package br.tiagohm.nestalgia.ui

import java.util.*

fun base64Encode(text: String): String {
    return Base64.getEncoder().encodeToString(text.toByteArray())
}

fun base64Decode(text: String): String {
    return String(Base64.getDecoder().decode(text.toByteArray()))
}

val operatingSystem by lazy {
    val os = System.getProperty("os.name", "generic").lowercase(Locale.ENGLISH)

    if (os.contains("mac") || os.contains("darwin")) {
        "MACOSX"
    } else if (os.contains("win")) {
        "WINDOWS"
    } else if (os.contains("nux")) {
        "LINUX"
    } else {
        "UNKNOWN"
    }
}