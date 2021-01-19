package br.tiagohm.nestalgia.core

interface AudioDevice : Disposable {
    fun play(buffer: ShortArray, length: Int, sampleRate: Int, isStereo: Boolean)

    fun stop()

    fun pause()

    fun processEndOfFrame()
}