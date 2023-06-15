package br.tiagohm.nestalgia.desktop.gui.home

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.MouseButton.*
import br.tiagohm.nestalgia.desktop.app.Preferences
import br.tiagohm.nestalgia.desktop.audio.Speaker
import br.tiagohm.nestalgia.desktop.gui.AbstractWindow
import br.tiagohm.nestalgia.desktop.gui.about.AboutWindow
import br.tiagohm.nestalgia.desktop.gui.cheats.CheatsWindow
import br.tiagohm.nestalgia.desktop.gui.settings.SettingsWindow
import br.tiagohm.nestalgia.desktop.helper.resource
import br.tiagohm.nestalgia.desktop.input.GamepadInputAction
import br.tiagohm.nestalgia.desktop.input.GamepadInputListener
import br.tiagohm.nestalgia.desktop.input.GamepadInputProvider
import br.tiagohm.nestalgia.desktop.input.MouseKeyboard
import br.tiagohm.nestalgia.desktop.video.Television
import javafx.beans.InvalidationListener
import javafx.event.ActionEvent
import javafx.fxml.FXML
import javafx.scene.control.Menu
import javafx.scene.control.MenuBar
import javafx.scene.control.MenuItem
import javafx.scene.control.ToggleGroup
import javafx.scene.input.KeyCombination
import javafx.scene.input.KeyEvent
import javafx.scene.input.MouseButton.*
import javafx.scene.input.MouseButton.MIDDLE
import javafx.scene.input.MouseEvent
import javafx.stage.FileChooser
import javafx.stage.FileChooser.*
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.stereotype.Component
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.imageio.ImageIO
import kotlin.io.path.*

@Component
class HomeWindow(@Autowired @Qualifier("primaryStage") override val window: Stage) : AbstractWindow(),
    GamepadInputListener, NotificationListener, BatteryProvider {

    override val resourceName = "Home"

    @Autowired private lateinit var console: Console
    @Autowired private lateinit var preferences: Preferences
    @Autowired private lateinit var saveDir: Path
    @Autowired private lateinit var screenshotDir: Path
    @Autowired private lateinit var settingsWindow: SettingsWindow
    @Autowired private lateinit var cheatsWindow: CheatsWindow
    @Autowired private lateinit var aboutWindow: AboutWindow
    @Autowired private lateinit var configurableApplicationContext: ConfigurableApplicationContext

    @FXML private lateinit var menuBar: MenuBar
    @FXML private lateinit var recentGamesMenu: Menu
    @FXML private lateinit var restoreStateMenu: Menu
    @FXML private lateinit var openRestoreStateMenuItem: MenuItem
    @FXML private lateinit var regionToggleGroup: ToggleGroup
    @FXML private lateinit var speedToggleGroup: ToggleGroup
    @FXML private lateinit var insertCoin1MenuItem: MenuItem
    @FXML private lateinit var insertCoin2MenuItem: MenuItem
    @FXML private lateinit var television: Television

    private lateinit var speaker: Speaker
    private lateinit var emulator: Emulator
    private lateinit var mouseKeyboard: MouseKeyboard

    override fun onCreate() {
        title = "Nestalgia"

        window.scene.setOnKeyPressed(::onKeyPressed)
        window.scene.setOnKeyReleased(::onKeyReleased)

        television.setOnMousePressed(::onMousePressed)
        television.setOnMouseReleased(::onMouseReleased)

        console.notificationManager.registerNotificationListener(this)
        console.batteryManager.registerProvider(this)

        val gamepadInputProvider = GamepadInputProvider(console, this)
        speaker = Speaker(console)
        val inputProviders = listOf(gamepadInputProvider)
        mouseKeyboard = MouseKeyboard(console, television)
        emulator = Emulator(console, speaker, television, mouseKeyboard, inputProviders)

        regionToggleGroup.selectToggle(regionToggleGroup.toggles[preferences.settings.region.ordinal])
        speedToggleGroup.selectToggle(speedToggleGroup.toggles[SPEEDS.indexOf(preferences.settings.emulationSpeed())])

        window.fullScreenProperty().addListener(InvalidationListener {
            if (window.isFullScreen) {
                menuBar.isManaged = false
                menuBar.opacity = 0.1
            } else {
                menuBar.isManaged = true
                menuBar.opacity = 1.0
            }
        })
    }

    override fun onStart() {
        loadRecentlyOpenGames()
    }

    override fun onClose() {
        configurableApplicationContext.close()
        emulator.close()
    }

    @FXML
    private fun openSettings() {
        console.pause()
        settingsWindow.showAndWait(this)
        console.resume()
    }

    @FXML
    private fun openCheats() {
        cheatsWindow.showAndWait(this)

        if (cheatsWindow.saved) {
            emulator.cheats(cheatsWindow.selectedCheats)
        }
    }

    override fun onAction(action: GamepadInputAction) {}

    override fun processNotification(type: NotificationType, vararg data: Any?) {}

    override fun loadBattery(name: String): IntArray {
        val path = Path.of("$saveDir", name)

        return try {
            LOG.info("loading battery. path={}", path)
            // TODO: Avoid read bytes.
            path.readBytes().toIntArray()
        } catch (e: NoSuchFileException) {
            LOG.warn("no battery found")
            IntArray(0)
        } catch (e: Throwable) {
            LOG.error("failed to load battery", e)
            IntArray(0)
        }
    }

    override fun saveBattery(name: String, data: IntArray) {
        val path = Path.of("$saveDir", name)

        try {
            LOG.info("saving battery. path={}, size={}", path, data.size)

            path.outputStream().buffered().use {
                data.forEach(it::write)
                it.flush()
            }
        } catch (e: Throwable) {
            LOG.error("failed to save battery", e)
        }
    }

    @FXML
    private fun openROM() {
        val chooser = FileChooser()

        val loadRomDir = preferences.loadRomDir
            .takeIf { it.isNotBlank() }
            ?.let { Path.of(it) }
            ?.takeIf { it.exists() && it.isDirectory() }

        if (loadRomDir != null) chooser.initialDirectory = loadRomDir.toFile()
        chooser.extensionFilters.add(ExtensionFilter("All ROM files", "*.nes", "*.fds", "*.unf"))
        chooser.extensionFilters.add(ExtensionFilter("NES ROM files", "*.nes"))
        chooser.extensionFilters.add(ExtensionFilter("Famicom ROM files", "*.fds"))
        chooser.extensionFilters.add(ExtensionFilter("UNIF ROM files", "*.unf"))

        openROM(chooser.showOpenDialog(window)?.toPath() ?: return)
    }

    private fun openROM(path: Path) {
        val name = path.nameWithoutExtension

        emulator.debugRun()

        loadConsolePreferences()

        if (emulator.load(path.readBytes().toIntArray(), name, FDS_BIOS)) {
            preferences.loadRomDir = "${path.parent}"
            preferences.save()
            loadSavedStates()
            saveGameToRecentlyOpened(path)
            insertCoin1MenuItem.isDisable = !emulator.isVsSystem
            insertCoin2MenuItem.isDisable = !emulator.isVsSystem
            title = "Nestalgia · $name"
        }
    }

    @FXML
    private fun saveState() {
        if (!emulator.running) return

        val hash = emulator.info.hash.md5
        val fileName = "$hash.${System.currentTimeMillis()}.nst"
        val filePath = Path.of("$saveDir", fileName)
        filePath.outputStream().use(emulator::saveState)

        loadSavedStates()
    }

    @FXML
    private fun openRestoreState() {
        if (!emulator.running) return

        val chooser = FileChooser()
        val hash = emulator.info.hash.md5

        chooser.initialDirectory = saveDir.toFile()
        chooser.extensionFilters.add(ExtensionFilter("Running game saved state files", "*$hash.*.nst"))
        chooser.extensionFilters.add(ExtensionFilter("All saved state files", "*.nst"))
        val file = chooser.showOpenDialog(window)?.toPath() ?: return

        file.inputStream().use(emulator::restoreState)
    }

    @FXML
    private fun takeScreenshot() {
        val image = emulator.takeScreenshot() ?: return
        val name = emulator.info.name
        val fileName = "$name.${System.currentTimeMillis()}.png"
        val filePath = Path.of("$screenshotDir", fileName)
        ImageIO.write(image, "png", filePath.toFile())
    }

    @FXML
    private fun hardReset() {
        emulator.reset(false)
    }

    @FXML
    private fun softReset() {
        emulator.reset(true)
    }

    @FXML
    private fun pause() {
        emulator.pause()
    }

    @FXML
    private fun resume() {
        emulator.resume()
    }

    @FXML
    private fun powerOff() {
        emulator.stop()
    }

    @FXML
    private fun chooseRegion(event: ActionEvent) {
        emulator.settings.region = Region.valueOf((event.source as MenuItem).userData as String)
        preferences.save()
    }

    @FXML
    private fun chooseSpeed(event: ActionEvent) {
        val speed = (event.source as MenuItem).userData as String
        emulator.settings.emulationSpeed(speed.toInt())
        preferences.save()
    }

    @FXML
    private fun toggleFullscreen() {
        window.isFullScreen = !window.isFullScreen
    }

    @FXML
    private fun insertCoin(event: ActionEvent) {
        val port = (event.source as MenuItem).userData as String
        emulator.insertCoin(port.toInt())
    }

    @FXML
    private fun debugContinue() {
        emulator.debugRun()
    }

    @FXML
    private fun debugRunOnePPUCycle() {
        emulator.debugRunPpuCycle()
    }

    @FXML
    private fun debugRunOneScanline() {
        emulator.debugRunScanline()
    }

    @FXML
    private fun debugRunOneFrame() {
        emulator.debugRunFrame()
    }

    @FXML
    private fun debugRunOneCpuCycle() {
        emulator.debugRunCpuCycle()
    }

    @FXML
    private fun openAbout() {
        aboutWindow.showAndWait(this)
    }

    private fun onKeyPressed(event: KeyEvent) {
        mouseKeyboard.onKeyPressed(event.code.code)
    }

    private fun onKeyReleased(event: KeyEvent) {
        mouseKeyboard.onKeyReleased(event.code.code)
    }

    private fun onMousePressed(event: MouseEvent) {
        val x = (event.x / television.width * Ppu.SCREEN_WIDTH).toInt()
        val y = (event.y / television.height * Ppu.SCREEN_HEIGHT).toInt()
        mouseKeyboard.onMousePressed(event.mouseButton, x, y)
    }

    private fun onMouseReleased(event: MouseEvent) {
        mouseKeyboard.onMouseReleased(event.mouseButton)
    }

    private fun loadConsolePreferences() {
        // Copy global settings to console settings.
        preferences.settings.copyTo(console.settings)

        if (console.settings.port1.type == ControllerType.NONE) {
            console.settings.port1.type = NES_CONTROLLER
            console.settings.markAsNeedControllerUpdate()
        }
        if (console.settings.port2.type == ControllerType.NONE) {
            console.settings.port2.type = NES_CONTROLLER
            console.settings.markAsNeedControllerUpdate()
        }

        if (console.settings.port1.keyMapping.isEmpty()) {
            KeyMapping.arrowKeys().copyTo(console.settings.port1.keyMapping)
            console.settings.markAsNeedControllerUpdate()
        }
        if (console.settings.port2.keyMapping.isEmpty()) {
            KeyMapping.wasd().copyTo(console.settings.port2.keyMapping)
            console.settings.markAsNeedControllerUpdate()
        }
    }

    private fun loadSavedStates() {
        if (!emulator.running) return

        val hash = emulator.info.hash.md5

        restoreStateMenu.items.clear()
        restoreStateMenu.items.add(openRestoreStateMenuItem)

        val files = saveDir
            .listDirectoryEntries("*$hash.*.nst")
            .sortedDescending()

        var i = 0

        while (i < files.size && i < 9) {
            val file = files[i++]
            val saveAt = OffsetDateTime.ofInstant(Instant.ofEpochMilli(file.name.split(".")[1].toLong()), ZoneId.systemDefault())
            val item = MenuItem(saveAt.format(DATE_TIME_FORMAT))
            item.accelerator = KeyCombination.valueOf("Ctrl + $i")
            item.setOnAction { file.inputStream().use(emulator::restoreState) }
            restoreStateMenu.items.add(item)
        }

        while (i < files.size) {
            files[i++].deleteIfExists()
        }
    }

    private fun loadRecentlyOpenGames() {
        recentGamesMenu.items.clear()

        val recentGames = preferences.recentlyOpened
            .filterNotNull()
            .filter { it.exists() && !it.isDirectory() }

        if (recentGames.size != preferences.recentlyOpened.size) {
            preferences.recentlyOpened.fill(null)

            for (i in recentGames.indices) {
                preferences.recentlyOpened[i] = recentGames[i]
            }

            preferences.save()
        }

        for (recentGame in recentGames) {
            val item = MenuItem(recentGame.nameWithoutExtension)
            item.setOnAction { openROM(recentGame) }
            recentGamesMenu.items.add(item)
        }
    }

    private fun saveGameToRecentlyOpened(path: Path) {
        for (i in preferences.recentlyOpened.size - 1 downTo 1) {
            preferences.recentlyOpened[i] = preferences.recentlyOpened[i - 1]
        }

        while (true) {
            val index = preferences.recentlyOpened.lastIndexOf(path)

            if (index >= 1) {
                for (i in index until preferences.recentlyOpened.size - 1) {
                    preferences.recentlyOpened[i] = preferences.recentlyOpened[i + 1]
                }

                preferences.recentlyOpened[preferences.recentlyOpened.size - 1] = null
            } else {
                break
            }
        }

        preferences.recentlyOpened[0] = path

        preferences.save()

        loadRecentlyOpenGames()
    }

    companion object {

        @JvmStatic private val LOG = LoggerFactory.getLogger(HomeWindow::class.java)
        @JvmStatic private val SPEEDS = intArrayOf(100, 200, 300, 50, 25)
        @JvmStatic private val DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

        @JvmStatic
        private val MouseEvent.mouseButton
            get() = when (button) {
                MIDDLE -> MouseButton.MIDDLE
                SECONDARY -> RIGHT
                else -> LEFT
            }

        @JvmStatic val FDS_BIOS by lazy {
            resource(FdsBios.NINTENDO_FDS_FILENAME)
                ?.use { it.readBytes().toIntArray() }
                ?: IntArray(0)
        }
    }
}
