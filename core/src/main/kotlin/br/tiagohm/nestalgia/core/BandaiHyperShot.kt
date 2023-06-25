package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.BandaiHyperShotButton.*
import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.Zapper.Companion.isLight

class BandaiHyperShot(console: Console, keyMapping: KeyMapping) : StandardController(console, BANDAI_HYPER_SHOT, EXP_DEVICE_PORT, keyMapping) {

    private var hyperShotStateBuffer = 0
    private val fireKey = keyMapping.key(FIRE)
    private val aimOffscreenKey = keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY)
    private var x = 0
    private var y = 0

    override fun refreshStateBuffer() {
        hyperShotStateBuffer = value
    }

    override fun setStateFromInput() {
        super.setStateFromInput()

        if (console.settings.isInputEnabled && console.keyManager.isKeyPressed(fireKey)) {
            setBit(FIRE)
        }

        if (console.keyManager.isKeyPressed(aimOffscreenKey)) {
            x = 0
            y = 0
        } else {
            x = console.keyManager.mouseX
            y = console.keyManager.mouseY
        }
    }

    val isLight
        get() = console.ppu.isLight(x, y, console.settings.zapperDetectionRadius[port])

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4016) {
            strobeOnRead()

            val output = hyperShotStateBuffer and 0x01 shl 1
            hyperShotStateBuffer = hyperShotStateBuffer shr 1
            output
        } else {
            (if (isLight) 0 else 0x08) or if (isPressed(FIRE)) 0x10 else 0x00
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("hyperShotStateBuffer", hyperShotStateBuffer)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        hyperShotStateBuffer = s.readInt("hyperShotStateBuffer")
    }

    companion object {

        const val AIM_OFFSCREEN_CUSTOM_KEY = 254
    }
}
