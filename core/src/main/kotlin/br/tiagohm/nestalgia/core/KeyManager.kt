package br.tiagohm.nestalgia.core

interface KeyManager {

    fun isKeyPressed(key: Key): Boolean

    fun refreshKeyState()

    val mouseX: Int

    val mouseY: Int
}
