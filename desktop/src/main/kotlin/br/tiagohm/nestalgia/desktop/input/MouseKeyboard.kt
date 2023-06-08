package br.tiagohm.nestalgia.desktop.input

import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.KeyManager
import br.tiagohm.nestalgia.core.MouseButton
import br.tiagohm.nestalgia.core.RenderingDevice

data class MouseKeyboard(
    private val console: Console,
    private val renderer: RenderingDevice,
) : KeyManager {

    private val keyPressed = BooleanArray(65536)
    private val mouseButtons = BooleanArray(3)

    override var mouseX = -1
        private set

    override var mouseY = -1
        private set

    override fun isKeyPressed(keyCode: Int): Boolean {
        return keyPressed[keyCode]
    }

    override fun isMouseButtonPressed(mouseButton: MouseButton): Boolean {
        return mouseButtons[mouseButton.ordinal]
    }

    override fun refreshKeyState() {}

    internal fun onKeyPressed(keyCode: Int) {
        keyPressed[keyCode] = true
    }

    internal fun onKeyReleased(keyCode: Int) {
        keyPressed[keyCode] = false
    }

    internal fun onMousePressed(button: MouseButton, x: Int, y: Int) {
        mouseButtons[button.ordinal] = true
        this.mouseX = x
        this.mouseY = y
    }

    internal fun onMouseReleased(button: MouseButton) {
        mouseButtons[button.ordinal] = false
    }
}
