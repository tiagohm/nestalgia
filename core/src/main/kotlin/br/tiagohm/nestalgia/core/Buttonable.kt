package br.tiagohm.nestalgia.core

interface Buttonable<in B : Button> {
    fun isPressed(button: B): Boolean

    fun buttonDown(button: B)

    fun buttonUp(button: B)
}