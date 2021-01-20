package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.KeyManager
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.StandardControllerButton
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

@ExperimentalUnsignedTypes
class StandardControllerConfig(
    port: Int,
    private val keyManager: KeyManager,
    private val keyMapping: KeyMapping,
    private val onOk: (KeyMapping) -> Unit,
) :
    Dialog("Standard Controller Settings - Port $port") {

    private var up = keyMapping.getKey(StandardControllerButton.UP)
    private var right = keyMapping.getKey(StandardControllerButton.RIGHT)
    private var left = keyMapping.getKey(StandardControllerButton.LEFT)
    private var down = keyMapping.getKey(StandardControllerButton.DOWN)
    private var select = keyMapping.getKey(StandardControllerButton.SELECT)
    private var start = keyMapping.getKey(StandardControllerButton.START)
    private var a = keyMapping.getKey(StandardControllerButton.A)
    private var b = keyMapping.getKey(StandardControllerButton.B)

    override val body: JPanel
        get() {
            return panel(9, 3, margin = margin(16)) {
                var row = 0

                label("Up", row, 0)
                dropdown(row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(up),
                    colSpan = 2,
                    onChanged = {
                        up = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("Down", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(down),
                    colSpan = 2,
                    onChanged = {
                        down = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("Left", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(left),
                    colSpan = 2,
                    onChanged = {
                        left = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("Right", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(right),
                    colSpan = 2,
                    onChanged = {
                        right = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("A", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(a),
                    colSpan = 2,
                    onChanged = {
                        a = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("B", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(b),
                    colSpan = 2,
                    onChanged = {
                        b = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("Select", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(select),
                    colSpan = 2,
                    onChanged = {
                        select = keyManager.getKeyCode(it)
                    }
                )

                row++

                label("Start", row, 0)
                dropdown(
                    row, 1,
                    keyManager.keyNames,
                    keyManager.getKeyName(start),
                    colSpan = 2,
                    onChanged = {
                        start = keyManager.getKeyCode(it)
                    }
                )

                row++

                button(
                    "Reset",
                    row, 1,
                    fill = Fill.HORIZONTAL,
                    anchor = Anchor.WEST,
                    onClick = ::reset
                )
                button(
                    "OK",
                    row, 2,
                    fill = Fill.HORIZONTAL,
                    anchor = Anchor.WEST,
                    onClick = ::onOk
                )
            }
        }

    private fun onOk() {
        val keyMapping = KeyMapping(a, b, up, down, left, right, start, select)
        onOk(keyMapping)
        dispose()
    }

    private fun reset() {
        up = keyMapping.getKey(StandardControllerButton.UP)
        right = keyMapping.getKey(StandardControllerButton.RIGHT)
        left = keyMapping.getKey(StandardControllerButton.LEFT)
        down = keyMapping.getKey(StandardControllerButton.DOWN)
        select = keyMapping.getKey(StandardControllerButton.SELECT)
        start = keyMapping.getKey(StandardControllerButton.START)
        a = keyMapping.getKey(StandardControllerButton.A)
        b = keyMapping.getKey(StandardControllerButton.B)
        updateView()
    }

    companion object {
        fun show(port: Int, keyManager: KeyManager, keyMapping: KeyMapping, onOk: (KeyMapping) -> Unit) {
            val dialog = StandardControllerConfig(port, keyManager, keyMapping, onOk)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}