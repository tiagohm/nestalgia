package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.CheatDatabase
import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.GameDatabase
import java.awt.EventQueue
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Paths
import java.util.stream.Collectors
import javax.swing.UIManager
import javax.swing.filechooser.FileSystemView

@ExperimentalUnsignedTypes
fun main() {
    System.setProperty("sun.java2d.opengl", "True")

    try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (e: Exception) {
        System.err.println("Unable to set native look and feel: $e")
    }

    val homeDir: String = FileSystemView.getFileSystemView().defaultDirectory.path

    val appDir = when (operatingSystem) {
        "LINUX" -> Paths.get(homeDir, ".config", "nestalgia")
        "WINDOWS" -> Paths.get(homeDir, "Nestalgia")
        "MACOSX" -> Paths.get(homeDir, "Documents", "Nestalgia")
        else -> throw IllegalArgumentException("Invalid operating system")
    }.toFile().also { it.mkdir() }

    val screenshotDir = Paths.get(appDir.path, "screenshots").toFile().also { it.mkdir() }
    val saveDir = Paths.get(appDir.path, "saves").toFile().also { it.mkdir() }
    val configFile = Paths.get(appDir.path, "config").toFile().also { if (!it.exists()) it.createNewFile() }
    val preferences = Preferences(configFile)

    System.setProperty("app.dir", appDir.path)
    System.setProperty("app.screenshotDir", screenshotDir.path)
    System.setProperty("app.saveDir", saveDir.path)

    System.err.println("HOME DIR: $appDir")

    val loader = Thread.currentThread().contextClassLoader

    loader.getResourceAsStream(GameDatabase.NES_DB_FILENAME)?.use {
        val lines = BufferedReader(InputStreamReader(it, Charsets.UTF_8)).lines().collect(Collectors.toList())
        GameDatabase.load(lines)
    }

    loader.getResourceAsStream(CheatDatabase.CHEAT_DB_FILENAME)?.use {
        val lines = BufferedReader(InputStreamReader(it, Charsets.UTF_8)).lines().collect(Collectors.toList())
        CheatDatabase.load(lines)
    }

    val console = Console()
    val renderer = Renderer(console)
    val speaker = Speaker(console)
    val mouseKeyboard = MouseKeyboard(console, renderer)

    EventQueue.invokeLater {
        val nestalgia = Nestalgia(
            console,
            speaker,
            renderer,
            mouseKeyboard,
            preferences,
        )

        nestalgia.isVisible = true
    }
}
