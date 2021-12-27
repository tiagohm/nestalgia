package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.core.EmulationFlag
import br.tiagohm.nestalgia.core.Emulator
import br.tiagohm.nestalgia.ui.*
import javax.swing.JPanel

class FdsConfig(
    private val emulator: Emulator,
    private val onSave: () -> Unit,
) : Dialog("FDS Settings") {

    private val settings = emulator.settings
    private var autoLoadDisk = settings.checkFlag(EmulationFlag.FDS_AUTO_LOAD_DISK)
    private var autoInsertDisk = settings.checkFlag(EmulationFlag.FDS_AUTO_INSERT_DISK)

    override val body: JPanel
        get() {
            return panel(3, 3, margin = margin(16)) {
                var row = 0

                checkbox(
                    row, 0,
                    autoLoadDisk,
                    "Automatically insert disk 1 side A when starting FDS games",
                    onChanged = {
                        autoLoadDisk = it
                    })

                checkbox(
                    ++row,
                    0,
                    autoInsertDisk,
                    "Automatically switch disks for FDS games",
                    onChanged = {
                        autoInsertDisk = it
                    })

                button(
                    "Save",
                    ++row, 0,
                    colSpan = 3,
                    fill = Fill.NONE,
                    anchor = Anchor.CENTER,
                    minimumSize = Size(100),
                    onClick = ::onOk
                )
            }
        }

    private fun onOk() {
        if (autoLoadDisk) settings.setFlag(EmulationFlag.FDS_AUTO_LOAD_DISK)
        else settings.clearFlag(EmulationFlag.FDS_AUTO_LOAD_DISK)

        if (autoInsertDisk) settings.setFlag(EmulationFlag.FDS_AUTO_INSERT_DISK)
        else settings.clearFlag(EmulationFlag.FDS_AUTO_INSERT_DISK)

        onSave()

        dispose()
    }

    companion object {

        fun show(emulator: Emulator, onSave: () -> Unit) {
            val dialog = FdsConfig(emulator, onSave)
            dialog.initialize()
            dialog.setLocationRelativeTo(null)
            dialog.isVisible = true
        }
    }
}