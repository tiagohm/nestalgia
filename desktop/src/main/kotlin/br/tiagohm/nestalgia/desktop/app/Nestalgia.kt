package br.tiagohm.nestalgia.desktop.app

import br.tiagohm.nestalgia.core.CheatDatabase
import br.tiagohm.nestalgia.core.GameDatabase
import br.tiagohm.nestalgia.desktop.*
import br.tiagohm.nestalgia.desktop.gui.home.HomeWindow
import br.tiagohm.nestalgia.desktop.helper.resource
import javafx.application.Application
import javafx.application.Platform
import javafx.stage.Stage
import kotlin.concurrent.thread

class Nestalgia : Application() {

    override fun start(primaryStage: Stage) {
        application = this

        thread(isDaemon = true) {
            resource(GameDatabase.FILENAME)?.use {
                GameDatabase.load(it.bufferedReader().lines())
            }
        }

        thread(isDaemon = true) {
            resource(CheatDatabase.FILENAME)?.use {
                CheatDatabase.load(it.bufferedReader().lines())
            }
        }

        Platform.runLater(settingsWindow::setUp)
        Platform.runLater(cheatsWindow::setUp)
        Platform.runLater(aboutWindow::setUp)

        homeWindow = HomeWindow(primaryStage)
        homeWindow.setUp()
        homeWindow.show()
    }
}
