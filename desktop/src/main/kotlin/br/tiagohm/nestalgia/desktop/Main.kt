package br.tiagohm.nestalgia.desktop

import br.tiagohm.nestalgia.desktop.app.Nestalgia
import javafx.application.Application
import oshi.PlatformEnum
import oshi.SystemInfo
import java.nio.file.Path
import java.util.*
import javax.swing.filechooser.FileSystemView
import kotlin.io.path.createDirectories

fun initAppDirectory(): Path? {
    val appDirectory = when (SystemInfo.getCurrentPlatform()) {
        PlatformEnum.LINUX -> {
            val userHomeDir = Path.of(System.getProperty("user.home"))
            Path.of("$userHomeDir", ".nestalgia")
        }
        PlatformEnum.WINDOWS -> {
            val documentsDir = FileSystemView.getFileSystemView().defaultDirectory.path
            Path.of(documentsDir, "Nestalgia")
        }
        else -> return null
    }

    appDirectory.createDirectories()
    System.setProperty("app.dir", "$appDirectory")
    return appDirectory
}

fun main(args: Array<String>) {
    System.setProperty("sun.java2d.opengl", "true")
    System.setProperty("prism.lcdtext", "false")

    initAppDirectory()

    // Sets default locale to en_US.
    Locale.setDefault(Locale.ENGLISH)

    // Run the JavaFX application.
    Application.launch(Nestalgia::class.java, *args)
}
