package br.tiagohm.nestalgia.desktop.input

import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyManager
import br.tiagohm.nestalgia.core.MouseButton
import br.tiagohm.nestalgia.core.Resetable

class MouseKeyboard : KeyManager, Resetable {

    private val keyPressed = BooleanArray(65536)
    private val mouseButtons = BooleanArray(4)

    @Volatile override var mouseX = -1
        private set

    @Volatile override var mouseY = -1
        private set

    @Volatile override var mouseDx = 0
        get() = field.also { field = 0 }
        private set

    @Volatile override var mouseDy = 0
        get() = field.also { field = 0 }
        private set

    @Volatile private var initialMouseMove = false

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
        if (!initialMouseMove) {
            initialMouseMove = true
            mouseDx = 0
            mouseDy = 0
        } else {
            mouseDx += x - mouseX
            mouseDy += y - mouseY
        }

        mouseX = x
        mouseY = y
    }

    override fun reset(softReset: Boolean) {
        initialMouseMove = false
    }
}
