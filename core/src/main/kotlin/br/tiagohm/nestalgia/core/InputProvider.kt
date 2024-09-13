package br.tiagohm.nestalgia.core

fun interface InputProvider {

    fun setInput(device: ControlDevice): Boolean
}
