package br.tiagohm.nestalgia.desktop

import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.EmulationSettings
import br.tiagohm.nestalgia.desktop.app.Nestalgia
import br.tiagohm.nestalgia.desktop.app.Preferences
import br.tiagohm.nestalgia.desktop.gui.about.AboutWindow
import br.tiagohm.nestalgia.desktop.gui.cheats.CheatsWindow
import br.tiagohm.nestalgia.desktop.gui.home.HomeWindow
import br.tiagohm.nestalgia.desktop.gui.settings.SettingsWindow
import javafx.application.Application
import oshi.PlatformEnum.LINUX
import oshi.PlatformEnum.WINDOWS
import oshi.SystemInfo.getCurrentPlatform
import java.nio.file.Path
import java.util.*
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.createDirectories
import kotlin.system.exitProcess

// Paths
lateinit var appDir: Path
lateinit var screenshotDir: Path
lateinit var saveDir: Path

// Application
lateinit var application: Application
val globalSettings = EmulationSettings()
val consoleSettings = EmulationSettings()
val console = Console(settings = consoleSettings)
lateinit var preferences: Preferences
lateinit var homeWindow: HomeWindow
val settingsWindow = SettingsWindow()
val cheatsWindow = CheatsWindow()
val aboutWindow = AboutWindow()

private fun initAppDirectory() {
    appDir = when (getCurrentPlatform()) {
        LINUX -> {
            val userHomeDir = Path.of(System.getProperty("user.home"))
            Path.of("$userHomeDir", ".nestalgia")
        }
        WINDOWS -> {
            val documentsDir = FileSystemView.getFileSystemView().defaultDirectory.path
            Path.of(documentsDir, "Nestalgia")
        }
        else -> exitProcess(1)
    }

    appDir.createDirectories()
    System.setProperty("app.dir", "$appDir")
}

private fun initializeSubDirectories() {
    screenshotDir = Path.of("$appDir", "screenshots").createDirectories()
    saveDir = Path.of("$appDir", "saves").createDirectories()
}

fun main(args: Array<String>) {
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("prism.lcdtext", "false")

    initAppDirectory()
    initializeSubDirectories()

    preferences = Preferences(Path.of("$appDir", "config.nst"), globalSettings)

    // Sets default locale to en_US.
    Locale.setDefault(Locale.ENGLISH)

    // Run the JavaFX application.
    Application.launch(Nestalgia::class.java, *args)
}
