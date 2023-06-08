package br.tiagohm.nestalgia.desktop.gui.about

import br.tiagohm.nestalgia.desktop.BuildConfig
import br.tiagohm.nestalgia.desktop.gui.AbstractDialog
import javafx.fxml.FXML
import javafx.scene.control.Label
import org.springframework.stereotype.Component

@Component
class AboutWindow : AbstractDialog() {

    override val resourceName = "About"

    @FXML private lateinit var versionLabel: Label

    override fun onCreate() {
        title = "About"
        resizable = false

        versionLabel.text = "Version: ${BuildConfig.VERSION_CODE}"
    }
}
