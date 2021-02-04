package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.*
import br.tiagohm.nestalgia.ui.*
import br.tiagohm.nestalgia.ui.dialogs.*
import java.awt.BorderLayout
import java.awt.FileDialog
import java.awt.GraphicsDevice
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.*
import javax.imageio.ImageIO
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JOptionPane
import kotlin.concurrent.thread
import kotlin.math.min

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Nestalgia(
    private val console: Console,
    private val audio: Speaker,
    private val video: Renderer,
    private val keyManager: MouseKeyboard,
    private val preferences: Preferences,
) : JFrame(),
    BatteryProvider,
    NotificationListener {

    private val settings = console.settings
    private val gamepadInputProvider = GamepadInputProvider(console, ::onGamepadAction)
    private val inputProviders = listOf(gamepadInputProvider)
    private val emulator = Emulator(console, audio, video, keyManager, inputProviders)

    private val slots = ArrayList<Pair<File, Int>>(MAX_SLOTS)
    private var slot = -1
    private val cheats = ArrayList<CheatInfo>()
    private val statusBar = StatusBar(console)

    private val batchRoms = ArrayList<File>()
    private var batchTestThread: Thread? = null

    private var isFullscreen = false
    private lateinit var graphicsDevice: GraphicsDevice

    private var fdsBios = FDS_BIOS

    init {
        console.notificationManager.registerNotificationListener(this)
        console.batteryManager.provider = this

        loadPreferences()

        title = "Nestalgia"
        defaultCloseOperation = DISPOSE_ON_CLOSE
        layout = BorderLayout()
        add(video, BorderLayout.CENTER)

        add(statusBar, BorderLayout.SOUTH)

        getRootPane().registerKeyboardAction(
            ::onEscape,
            "Escape",
            key(KeyEvent.VK_ESCAPE),
            JComponent.WHEN_IN_FOCUSED_WINDOW
        )

        buildMenu()

        pack()
        setLocationRelativeTo(null)

        video.addMouseListener(keyManager)
        video.addMouseMotionListener(keyManager)
        video.addKeyListener(keyManager)
        video.isFocusable = true
        video.requestFocus()

        video.initialize()

        iconImage = APP_ICON
    }

    override fun dispose() {
        batchTestThread?.interrupt()
        batchTestThread = null
        statusBar.dispose()
        gamepadInputProvider.dispose()
        emulator.dispose()
        audio.dispose()
        super.dispose()
    }

    override fun processNotification(type: NotificationType, vararg data: Any?) {
        when (type) {
            NotificationType.GAME_STOPPED -> video.repaint()
            NotificationType.ERROR -> statusBar.showText(data[0] as String, type = StatusBar.Type.ERROR)
            else -> {
            }
        }
    }

    override fun saveBattery(name: String, data: UByteArray) {
        val saveDir = System.getProperty("app.saveDir")
        val filePath = Paths.get(saveDir, name)
        val file = filePath.toFile()

        try {
            file.writeBytes(data.toByteArray())
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }

    override fun loadBattery(name: String): UByteArray {
        val saveDir = System.getProperty("app.saveDir")
        val filePath = Paths.get(saveDir, name)

        return try {
            Files.readAllBytes(filePath).toUByteArray()
        } catch (e: Exception) {
            System.err.println(e.message)
            UByteArray(0)
        }
    }

    private fun onEscape(e: ActionEvent) {
        disableFullscreen()
    }

    private fun loadPreferences() {
        val snapshot = Snapshot()
        preferences.settings.saveState(snapshot)
        emulator.settings.restoreState(snapshot)

        if (emulator.settings.getControllerType(0) == ControllerType.NONE) {
            emulator.settings.setControllerType(0, ControllerType.STANDARD)
        }

        if (emulator.settings.getControllerKeys(0) == KeyMapping.NONE) {
            emulator.settings.setControllerKeys(0, DEFAULT_KEYS)
        }
    }

    private fun saveSettings() {
        val snapshot = Snapshot()
        emulator.settings.saveState(snapshot)
        preferences.settings.restoreState(snapshot)
        preferences.save()
    }

    private fun buildMenu() {
        if (isFullscreen) return

        menuBar {
            menu("File") {
                menuItem("Open ROM", ctrl(KeyEvent.VK_O), this@Nestalgia::loadROM)

                menu("Open Recent") {
                    val games = preferences.recentlyOpened

                    games.forEach {
                        if (it.isEmpty() || !it.contains(":")) return@forEach

                        val game = it.split(":")
                        val name = base64Decode(game[0])
                        val file = File(base64Decode(game[1]))

                        if (file.exists()) {
                            menuItem(name, null) {
                                emulator.debugRun()

                                val rom = file.readBytes()

                                if (emulator.load(rom, name, fdsBios)) {
                                    statusBar.hideText()
                                    loadSavedStates()
                                    saveGameToRecentlyOpened(file)
                                    title = "Nestalgia - $name"
                                }
                            }
                        }
                    }
                }

                separator()

                menuItem("Save State", ctrl(KeyEvent.VK_S), this@Nestalgia::saveState)

                menu("Restore State") {
                    for ((i, slot) in slots.withIndex()) {
                        menuItem(
                            DATETIME_FORMATTER.format(slot.first.lastModified()),
                            if (i < 10) ctrl(KeyEvent.VK_0 + i) else null
                        ) {
                            restoreState(slot.first)
                        }
                    }

                    menuItem("Open...", ctrl(KeyEvent.VK_R), this@Nestalgia::restoreState)
                }

                separator()

                menuItem("Screenshot", key(KeyEvent.VK_F12), this@Nestalgia::takeScreenshot)
            }
            menu("Game") {
                menuItem("Hard Reset", key(KeyEvent.VK_F5), emulator::powerCycle)
                menuItem("Soft Reset", key(KeyEvent.VK_F6), emulator::reset)
                menuItem("Pause", key(KeyEvent.VK_F7), emulator::pause)
                menuItem("Resume", key(KeyEvent.VK_F8), emulator::resume)
                menuItem("Power Off", key(KeyEvent.VK_F9), emulator::stop)

                separator()

                menu("Region") {
                    radioMenuItem("Auto", settings.region == Region.AUTO) { setRegion(Region.AUTO) }
                    radioMenuItem("NTSC", settings.region == Region.NTSC) { setRegion(Region.NTSC) }
                    radioMenuItem("PAL", settings.region == Region.PAL) { setRegion(Region.PAL) }
                    radioMenuItem("DENDY", settings.region == Region.DENDY) { setRegion(Region.DENDY) }
                }

                menu("Speed") {
                    radioMenuItem("Normal (100%)", settings.getEmulationSpeed() == 100) { setSpeed(100) }
                    radioMenuItem("Double (200%)", settings.getEmulationSpeed() == 200) { setSpeed(200) }
                    radioMenuItem("Triple (300%)", settings.getEmulationSpeed() == 300) { setSpeed(300) }
                    radioMenuItem("Half (50%)", settings.getEmulationSpeed() == 50) { setSpeed(50) }
                    radioMenuItem("Quarter (25%)", settings.getEmulationSpeed() == 25) { setSpeed(25) }
                }

                separator()

                menu("Settings") {
                    menuItem("Audio", action = this@Nestalgia::showAudioConfig)
                    menuItem("Controller", action = this@Nestalgia::showControllerConfig)
                    menuItem("Video", action = this@Nestalgia::showVideoConfig)
                    menuItem("Emulation", action = this@Nestalgia::showEmulationConfig)
                    menuItem("FDS", action = this@Nestalgia::showFdsConfig)
                }

                separator()

                menuItem("Cheats", key(KeyEvent.VK_F2), this@Nestalgia::showCheats)

                val sideCount = if (emulator.console.isFds) emulator.fdsSideCount else 0

                if (sideCount > 0) {
                    separator()

                    menuItem("Switch Disk Side", ctrl(KeyEvent.VK_B), emulator::switchDiskSide)

                    menu("Select Disk") {
                        for (i in 0 until sideCount) {
                            val side = if (i % 2 == 0) "A" else "B"
                            menuItem("Disk ${i / 2 + 1} Side $side") { emulator.insertDisk(i) }
                        }
                    }

                    menuItem("Eject Disk", action = emulator::ejectDisk)
                }
                separator()
                menuItem("Fullscreen", key(KeyEvent.VK_F11), this@Nestalgia::toggleFullscreen)
            }
            menu("Debug") {
                menuItem("Continue", shift(KeyEvent.VK_F5), emulator::debugRun)

                separator()

                menuItem("Run one PPU cycle", shift(KeyEvent.VK_F6), emulator::debugRunPpuCycle)
                menuItem("Run one scanline", shift(KeyEvent.VK_F7), emulator::debugRunScanline)
                menuItem("Run one frame", shift(KeyEvent.VK_F8), emulator::debugRunFrame)
                menuItem("Run one CPU cycle", shift(KeyEvent.VK_F9), emulator::debugRunCpuCycle)

                separator()

                menuItem("Break On...", action = this@Nestalgia::showBreakOnConfig)

                separator()

                menuItem("Batch Test...", action = this@Nestalgia::loadBatchTest)
            }
            menu("Help") {
                menuItem("About", key(KeyEvent.VK_F1)) {
                    val message = """
                        Nestalgia $VERSION_NAME

                        Cross-platform and high-accuracy NES/Famicom emulator built in Kotlin.

                        This project was ported from Mesen (https://github.com/SourMesen/Mesen).

                        Maintainer: Tiago Melo <tiago.henrique.cco@gmail.com>
                        GitHub: https://github.com/tiagohm/nestalgia
                        Website: https://tiagohm.github.io/nestalgia

                        This program is free software licensed under the GPL version 3, and comes with
                        NO WARRANTY of any kind. (but if something is broken, please report it).
                        See LICENSE file for details.
                    """.trimIndent()
                    JOptionPane.showMessageDialog(null, message)
                }
            }
        }
    }

    private fun onGamepadAction(action: GamepadInputProvider.Action) {
        when (action) {
            GamepadInputProvider.Action.X -> saveState()
            GamepadInputProvider.Action.Y -> takeScreenshot()
            GamepadInputProvider.Action.LB -> restorePreviousState()
            GamepadInputProvider.Action.RB -> restoreNextState()
        }
    }

    private fun loadSavedStates() {
        slots.clear()
        slot = -1

        val name = emulator.info.hash.sha1
        val saveDir = System.getProperty("app.saveDir")

        for (i in 0 until MAX_SLOTS) {
            val fileName = "$name." + String.format("%03d", i) + ".nst"
            val filePath = Paths.get(saveDir, fileName)
            val file = filePath.toFile()

            if (file.exists()) {
                slots.add(Pair(file, i))
            }
        }

        if (slots.isNotEmpty()) {
            slots.sortBy { -it.first.lastModified() }
            slot = -1
        }

        buildMenu()
    }

    private fun findAvailableSlot(): Int {
        for (i in 0 until MAX_SLOTS) if (!slots.any { it.second == i }) return i
        return -1
    }

    private fun setRegion(region: Region) {
        emulator.settings.region = region
        saveSettings()
    }

    private fun setSpeed(speed: Int) {
        emulator.settings.setEmulationSpeed(speed)
        saveSettings()
    }

    private fun showAudioConfig() {
        AudioConfig.show(emulator, ::saveSettings)
    }

    private fun showControllerConfig() {
        ControllerConfig.show(emulator, ::saveSettings)
    }

    private fun showVideoConfig() {
        VideoConfig.show(emulator, ::saveSettings)
    }

    private fun showEmulationConfig() {
        EmulationConfig.show(emulator, ::saveSettings)
    }

    private fun showFdsConfig() {
        FdsConfig.show(emulator, ::saveSettings)
    }

    private fun showCheats() {
        if (emulator.isRunning) {
            CheatDialog.show(emulator.info.hash.prgCrc32, cheats) {
                console.cheatManager.setCheats(cheats)
            }
        }
    }

    private fun showBreakOnConfig() {
        BreakOnConfig.show(console.debugger.breakOnType, console.debugger.breakOnCount) { type, count ->
            console.debugger.breakOn(type, count)
        }
    }

    private fun loadROM() {
        val dialog = FileDialog(this)
        dialog.mode = FileDialog.LOAD
        dialog.title = "Select a ROM to load"
        dialog.setFilenameFilter { _, name -> name.endsWith(".fds") || name.endsWith(".nes") }
        preferences.loadRomDir.takeIf { it.isNotEmpty() }?.let { dialog.directory = it }

        dialog.isVisible = true

        dialog.file?.let {
            preferences.loadRomDir = dialog.directory
            saveSettings()

            val file = File(dialog.directory + it)
            val name = file.nameWithoutExtension
            val data = file.readBytes()

            emulator.debugRun()

            if (emulator.load(data, name, fdsBios)) {
                statusBar.hideText()
                loadSavedStates()
                saveGameToRecentlyOpened(file)
                title = "Nestalgia - $name"
            }
        }
    }

    private fun loadBatchTest() {
        val dialog = FileDialog(this)
        dialog.mode = FileDialog.LOAD
        dialog.title = "Select the ROMs to test"
        dialog.isMultipleMode = true
        dialog.setFilenameFilter { _, name -> name.endsWith(".fds") || name.endsWith(".nes") }
        preferences.loadRomDir.takeIf { it.isNotEmpty() }?.let { dialog.directory = it }

        dialog.isVisible = true

        batchTestThread?.interrupt()

        dialog.files?.let {
            batchRoms.clear()
            batchRoms.addAll(it)

            emulator.debugRun()

            batchTestThread = thread {
                for (rom in batchRoms) {
                    val data = rom.readBytes()
                    val name = rom.nameWithoutExtension

                    if (emulator.load(data, name, fdsBios)) {
                        loadSavedStates()
                        title = "Nestalgia - $name"
                    } else {
                        continue
                    }

                    val factor = if (emulator.isFds) 2L else 1L
                    val time = BATCH_TEST_TIMEOUT * factor

                    Thread.sleep(time)

                    takeScreenshot(false, false)
                }

                emulator.stop()
            }
        }
    }

    private fun saveGameToRecentlyOpened(file: File) {
        val name = file.nameWithoutExtension
        val entry = "${base64Encode(name)}:${base64Encode(file.path)}"

        val games = preferences.recentlyOpened
        val gamesToSave = ArrayList<String>(games.size)

        for (game in games) {
            if (game.isNotEmpty() && game.contains(":")) {
                val path = base64Decode(game.split(":")[1])

                if (File(path).exists()) {
                    gamesToSave.add(game)
                }
            }
        }

        gamesToSave.remove(entry)
        gamesToSave.add(0, entry)

        for (i in 0 until min(gamesToSave.size, 10)) {
            preferences.recentlyOpened[i] = gamesToSave[i]
        }

        saveSettings()
        buildMenu()
    }

    private inline fun toggleFullscreen() {
        if (isFullscreen) disableFullscreen() else enableFullscreen()
    }

    private fun enableFullscreen(): Boolean {
        if (!isFullscreen) {
            graphicsDevice = graphicsConfiguration.device

            if (graphicsDevice.isFullScreenSupported) {
                graphicsDevice.fullScreenWindow = this
                isFullscreen = true
                jMenuBar = null
                return true
            }
        }

        return false
    }

    private fun disableFullscreen(): Boolean {
        return if (isFullscreen && ::graphicsDevice.isInitialized) {
            graphicsDevice.fullScreenWindow = null
            isFullscreen = false
            buildMenu()
            true
        } else {
            false
        }
    }

    private fun saveState() {
        if (emulator.isRunning) {
            val name = emulator.info.hash.sha1
            val saveDir = System.getProperty("app.saveDir")
            var slot = findAvailableSlot()

            if (slot == -1) {
                val a = slots[slots.size - 1]
                a.first.delete()
                slots.remove(a)
                slot = a.second
            }

            val fileName = "$name." + String.format("%03d", slot) + ".nst"
            val filePath = Paths.get(saveDir, fileName)
            val file = filePath.toFile()

            try {
                val data = emulator.saveState()
                file.writeBytes(data)
                slots.add(0, Pair(file, slot))
                this.slot = -1
                buildMenu()
                statusBar.showText("State saved at $file", type = StatusBar.Type.SUCCESS)
            } catch (e: Exception) {
                System.err.println(e.message)
            }
        }
    }

    @Synchronized
    private fun restoreNextState() {
        slot = if (slot > 0) slot - 1 else slots.size - 1
        restoreState(slot)
    }

    @Synchronized
    private fun restorePreviousState() {
        slot = (slot + 1) % slots.size
        restoreState(slot)
    }

    fun restoreState(slot: Int) {
        if (slot >= 0 && slot < slots.size) {
            restoreState(slots[slot].first)
        }
    }

    private fun restoreState() {
        if (emulator.isRunning) {
            try {
                val romName = emulator.info.name
                val dialog = FileDialog(this)
                dialog.mode = FileDialog.LOAD
                dialog.title = "Select a state to restore"
                dialog.setFilenameFilter { _, name -> name.startsWith(romName) && name.endsWith(".nst") }
                dialog.directory = System.getProperty("app.saveDir")

                dialog.isVisible = true

                dialog.file?.let {
                    val file = File(dialog.directory + it)
                    restoreState(file)
                }
            } catch (e: Exception) {
                System.err.println(e.message)
            }
        }
    }

    private inline fun restoreState(file: File) {
        emulator.debugRun()
        emulator.restoreState(file.readBytes())
        statusBar.showText("State restored from $file", type = StatusBar.Type.SUCCESS)
    }

    private fun takeScreenshot(
        hasSuffix: Boolean = true,
        showStatusBar: Boolean = true,
    ): File? {
        val data = emulator.takeScreenshot().takeIf { it.isNotEmpty() } ?: return null
        val screenshotDir = System.getProperty("app.screenshotDir")
        val name = emulator.info.name

        try {
            for (i in 1 until 1000) {
                val suffix = if (hasSuffix) String.format(".%03d", i) else ""
                val fileName = "$name$suffix.png"
                val filePath = Paths.get(screenshotDir, fileName)
                val file = filePath.toFile()

                if (!hasSuffix || !file.exists()) {
                    val image = BufferedImage(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT, BufferedImage.TYPE_INT_ARGB)
                    val imageData = (image.raster.dataBuffer as DataBufferInt).data
                    data.copyInto(imageData)
                    ImageIO.write(image, "png", file)

                    if (showStatusBar) {
                        statusBar.showText("Screenshot saved at $filePath", type = StatusBar.Type.SUCCESS)
                    }

                    return file
                }
            }
        } catch (e: Exception) {
            System.err.println(e.message)
        }

        return null
    }

    companion object {
        private val DATETIME_FORMATTER = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

        val DEFAULT_KEYS = KeyMapping(
            KeyEvent.VK_A, KeyEvent.VK_S, // A B
            KeyEvent.VK_UP, KeyEvent.VK_DOWN, KeyEvent.VK_LEFT, KeyEvent.VK_RIGHT, // UP DOWN LEFT RIGHT
            KeyEvent.VK_ENTER, KeyEvent.VK_SPACE, // START SELECT
        )

        val FDS_BIOS by lazy {
            Thread.currentThread().contextClassLoader.getResourceAsStream(FdsBios.NINTENDO_FDS_FILENAME)?.use {
                it.readBytes()
            } ?: ByteArray(0)
        }

        const val MAX_SLOTS = 30
        const val VERSION_NAME = "0.8.0"
        const val BATCH_TEST_TIMEOUT = 15000L
    }
}
