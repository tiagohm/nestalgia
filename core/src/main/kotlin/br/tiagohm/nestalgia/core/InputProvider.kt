package br.tiagohm.nestalgia.core

interface InputProvider {
    fun setInput(device: ControlDevice): Boolean
}