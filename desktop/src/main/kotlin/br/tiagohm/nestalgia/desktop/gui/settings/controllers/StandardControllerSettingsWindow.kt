package br.tiagohm.nestalgia.desktop.gui.settings.controllers

import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.Key
import br.tiagohm.nestalgia.core.KeyMapping
import br.tiagohm.nestalgia.core.StandardController
import javafx.fxml.FXML
import javafx.scene.control.ComboBox

open class StandardControllerSettingsWindow(
    override val keyMapping: KeyMapping,
    private val type: ControllerType,
) : AbstractControllerWindow<StandardController.Button>() {

    override val resourceName = "StandardControllerSettings"

    @FXML protected lateinit var upComboBox: ComboBox<Key>
    @FXML protected lateinit var downComboBox: ComboBox<Key>
    @FXML protected lateinit var leftComboBox: ComboBox<Key>
    @FXML protected lateinit var rightComboBox: ComboBox<Key>
    @FXML protected lateinit var startComboBox: ComboBox<Key>
    @FXML protected lateinit var selectComboBox: ComboBox<Key>
    @FXML protected lateinit var bComboBox: ComboBox<Key>
    @FXML protected lateinit var aComboBox: ComboBox<Key>
    @FXML protected var microphoneComboBox: ComboBox<Key>? = null
    @FXML protected var turboBComboBox: ComboBox<Key>? = null
    @FXML protected var turboAComboBox: ComboBox<Key>? = null
    @FXML override lateinit var presetComboBox: ComboBox<String>

    override lateinit var buttonComboBoxes: Array<ComboBox<Key>?>
    override val buttonEntries = StandardController.Button.entries

    override fun onCreate() {
        title = when (type) {
            HORI_TRACK -> "Hori Track"
            BANDAI_HYPER_SHOT -> "Bandai Hyper Shot"
            PACHINKO -> "Pachinko"
            else -> "NES/Famicom Controller"
        }

        buttonComboBoxes = arrayOf(
            upComboBox, downComboBox, leftComboBox, rightComboBox,
            startComboBox, selectComboBox, bComboBox, aComboBox,
            microphoneComboBox, turboBComboBox, turboAComboBox,
        )

        super.onCreate()
    }
}
