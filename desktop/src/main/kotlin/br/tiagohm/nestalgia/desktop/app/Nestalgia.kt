package br.tiagohm.nestalgia.desktop.app

import br.tiagohm.nestalgia.core.CheatDatabase
import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.EmulationSettings
import br.tiagohm.nestalgia.core.GameDatabase
import br.tiagohm.nestalgia.desktop.gui.home.HomeWindow
import br.tiagohm.nestalgia.desktop.helper.resource
import javafx.application.Application
import javafx.stage.Stage
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ConfigurableApplicationContext
import kotlin.concurrent.thread

class Nestalgia : Application() {

    override fun start(primaryStage: Stage) {
        val context = runApplication<App>(*parameters.raw.toTypedArray()) {
            addInitializers(ApplicationContextInitializer<ConfigurableApplicationContext> {
                val globalSettings = EmulationSettings()
                val consoleSettings = EmulationSettings()
                it.beanFactory.registerSingleton("hostServices", hostServices)
                it.beanFactory.registerSingleton("primaryStage", primaryStage)
                it.beanFactory.registerSingleton("globalSettings", globalSettings)
                it.beanFactory.registerSingleton("consoleSettings", consoleSettings)
                it.beanFactory.registerSingleton("console", Console(settings = consoleSettings))
            })
        }

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

        val homeWindow = context.getBean(HomeWindow::class.java)
        homeWindow.show()
    }
}
