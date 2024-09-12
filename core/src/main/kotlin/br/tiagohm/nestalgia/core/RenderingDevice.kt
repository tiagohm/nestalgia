package br.tiagohm.nestalgia.core

interface RenderingDevice : Resetable, AutoCloseable {

    fun updateFrame(buffer: IntArray, width: Int, height: Int)

    fun render()
}
