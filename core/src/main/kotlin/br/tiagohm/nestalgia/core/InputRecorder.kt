package br.tiagohm.nestalgia.core

import java.io.Closeable

interface InputRecorder : Closeable {

    fun recordInput(devices: Iterable<ControlDevice>)
}
