package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.Zapper.Button.FIRE

// https://wiki.nesdev.com/w/index.php/Zapper

class Zapper(
    console: Console, type: ControllerType, port: Int,
    private val keyMapping: KeyMapping,
) : ControlDevice(console, type, port) {

    enum class Button(override val bit: Int) : ControllerButton, HasCustomKey {
        FIRE(0);

        override val keyIndex = 0
    }

    @Volatile private var x = 0
    @Volatile private var y = 0

    private val fireKey = keyMapping.key(FIRE)
    private val aimOffscreenKey = keyMapping.customKey(AIM_OFFSCREEN_CUSTOM_KEY)

    override fun setStateFromInput() {
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
        if (isExpansionDevice && addr == 0x4017 || isCurrentPort(addr)) {
            return (if (isLight) 0x00 else 0x08) or if (isPressed(FIRE)) 0x10 else 0x00
        }

        return 0
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) = Unit

    companion object {

        const val AIM_OFFSCREEN_CUSTOM_KEY = 255

        internal fun Ppu.isLight(mx: Int, my: Int, radius: Int): Boolean {
            val scanline = scanline
            val cycle = cycle

            if (mx >= 0 && my >= 0) {
                for (a in -radius..radius) {
                    val y = my + a

                    if (y >= 0 && y < Ppu.SCREEN_HEIGHT) {
                        for (b in -radius..radius) {
                            val x = mx + b

                            if (x >= 0 && x < Ppu.SCREEN_WIDTH) {
                                if (scanline >= y &&
                                    (scanline - y <= 20) &&
                                    (scanline != y || cycle > x) &&
                                    pixelBrightnessAt(x, y) >= 85
                                ) {
                                    // Light cannot be detected if the Y/X position is further
                                    // ahead than the PPU, or if the PPU drew a dark color.
                                    return true
                                }
                            }
                        }
                    }
                }
            }

            return false
        }
    }
}
