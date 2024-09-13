package br.tiagohm.nestalgia.core

sealed interface VideoFilter : AutoCloseable {

    fun sendFrame(input: IntArray): IntArray

    fun takeScreenshot(): IntArray

    override fun close() = Unit
}
