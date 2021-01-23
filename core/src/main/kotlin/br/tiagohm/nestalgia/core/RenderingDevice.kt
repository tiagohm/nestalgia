package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface RenderingDevice : Resetable, Disposable {
    val screenWidth: Int

    val screenHeight: Int

    fun updateFrame(buffer: IntArray, width: Int, height: Int)

    fun render()
}