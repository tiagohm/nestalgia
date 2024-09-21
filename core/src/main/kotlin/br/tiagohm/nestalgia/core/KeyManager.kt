package br.tiagohm.nestalgia.core

interface KeyManager {

    fun isKeyPressed(key: Key): Boolean

    fun refreshKeyState()

    val mouseX: Int

    val mouseY: Int

    val mouseDx: Int

    val mouseDy: Int

    companion object : KeyManager {

        override fun isKeyPressed(key: Key) = false

        override fun refreshKeyState() = Unit

        override val mouseX = 0

        override val mouseY = 0

        override val mouseDx = 0

        override val mouseDy = 0
    }
}
