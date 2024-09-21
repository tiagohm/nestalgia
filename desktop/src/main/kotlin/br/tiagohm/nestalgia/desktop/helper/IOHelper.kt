@file:Suppress("NOTHING_TO_INLINE")

package br.tiagohm.nestalgia.desktop.helper

import java.io.InputStream
import java.net.URL

@JvmField val classLoader = Thread.currentThread().contextClassLoader!!

inline fun resourceUrl(name: String): URL? {
    return classLoader.getResource(name)
}

inline fun resource(name: String): InputStream? {
    return classLoader.getResourceAsStream(name)
}
