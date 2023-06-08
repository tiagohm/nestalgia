package br.tiagohm.nestalgia.core

import java.io.Closeable

interface VideoFilter : Closeable {

    fun sendFrame(input: IntArray): IntArray

    fun takeScreenshot(): IntArray

    override fun close() {}
}
