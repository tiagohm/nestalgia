package br.tiagohm.nestalgia.core

import java.io.Closeable

interface AudioDevice : Closeable {

    fun play(buffer: ShortArray, length: Int, sampleRate: Int, stereo: Boolean)

    fun stop()

    fun pause()

    fun processEndOfFrame()
}
