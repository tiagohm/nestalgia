package br.tiagohm.nestalgia.core

sealed interface InputRecorder : AutoCloseable {

    fun recordInput(devices: Iterable<ControlDevice>)
}
