package br.tiagohm.nestalgia.core

interface KeyManager {
    fun isKeyPressed(keyCode: Int): Boolean

    fun isMouseButtonPressed(mouseButton: MouseButton): Boolean

    fun refreshKeyState()

    val x: Int

    val y: Int

    val keyNames: List<String>

    fun getKeyName(keyCode: Int): String

    fun getKeyCode(name: String): Int
}