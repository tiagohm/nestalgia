package br.tiagohm.nestalgia.core

interface ControllerButton {

    val bit: Int

    val isCustomKey
        get() = false
}
