package br.tiagohm.nestalgia.core

interface AudioDevice : AutoCloseable {

    fun play(buffer: ShortArray, length: Int, sampleRate: Int, stereo: Boolean)

    fun stop()

    fun pause()

    fun processEndOfFrame()
}
