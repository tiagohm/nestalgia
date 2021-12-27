package br.tiagohm.nestalgia.core

interface InputRecorder : Disposable {
    fun recordInput(devices: Iterable<ControlDevice>)
}