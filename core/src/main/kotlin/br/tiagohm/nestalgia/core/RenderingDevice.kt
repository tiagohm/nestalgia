package br.tiagohm.nestalgia.core

import java.io.Closeable

interface RenderingDevice : Resetable, Closeable {

    fun updateFrame(buffer: IntArray, width: Int, height: Int)

    fun render()
}
