package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface InputProvider {
    fun setInput(device: ControlDevice): Boolean
}