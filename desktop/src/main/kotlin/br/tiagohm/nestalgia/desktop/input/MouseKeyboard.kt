package br.tiagohm.nestalgia.desktop.input

import br.tiagohm.nestalgia.core.*

data class MouseKeyboard(
    private val console: Console,
    private val renderer: RenderingDevice,
) : KeyManager {

    private val keyPressed = BooleanArray(65536)
    private val mouseButtons = BooleanArray(4)

    override var mouseX = -1
        private set

    override var mouseY = -1
        private set

    override fun isKeyPressed(key: Key): Boolean {
        return if (key is MouseButton) mouseButtons[key.code]
        else keyPressed[key.code]
    }

    override fun refreshKeyState() = Unit

    internal fun onKeyPressed(keyCode: Int) {
        keyPressed[keyCode] = true
    }

    internal fun onKeyReleased(keyCode: Int) {
        keyPressed[keyCode] = false
    }

    internal fun onMousePressed(button: MouseButton, x: Int, y: Int) {
        mouseButtons[button.code] = true
        mouseX = x
        mouseY = y
    }

    internal fun onMouseReleased(button: MouseButton) {
        mouseButtons[button.code] = false
    }

    internal fun onMouseMoved(x: Int, y: Int) {
        mouseX = x
        mouseY = y
    }
}
