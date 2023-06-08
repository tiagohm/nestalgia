package br.tiagohm.nestalgia.core

interface KeyManager {

    fun isKeyPressed(keyCode: Int): Boolean

    fun isMouseButtonPressed(mouseButton: MouseButton): Boolean

    fun refreshKeyState()

    val mouseX: Int

    val mouseY: Int
}
