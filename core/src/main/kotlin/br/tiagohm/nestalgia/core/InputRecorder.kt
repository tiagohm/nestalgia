package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface InputRecorder : Disposable {
    fun recordInput(devices: Iterable<ControlDevice>)
}