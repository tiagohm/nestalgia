package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Zapper

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
class Zapper(console: Console, port: Int) :
    ControlDevice(console, port),
    Buttonable<Zapper.Buttons> {

    enum class Buttons(override val bit: Int) : Button {
        FIRE(0)
    }

    var x = 0

    var y = 0

    override fun setStateFromInput() {
        if (console.keyManager == null) return

        if (console.settings.isInputEnabled && console.keyManager!!.isMouseButtonPressed(MouseButton.LEFT)) {
            buttonDown(Buttons.FIRE)
        }

        if (console.keyManager!!.isMouseButtonPressed(MouseButton.RIGHT)) {
            x = 0
            y = 0
        } else {
            x = console.keyManager!!.x
            y = console.keyManager!!.y
        }
    }

    @Synchronized
    override fun buttonDown(button: Buttons) {
        setBit(button.bit)
    }

    @Synchronized
    override fun buttonUp(button: Buttons) {
        clearBit(button.bit)
    }

    override fun isPressed(button: Buttons): Boolean {
        return isPressed(button.bit)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        var output: UByte = 0U

        if ((isExpansionDevice && addr.toUInt() == 0x4017U) || isCurrentPort(addr)) {
            output = ((if (isLight()) 0x00U else 0x08U) or (if (isPressed(Buttons.FIRE)) 0x10U else 0x00U)).toUByte()
        }

        return output
    }

    fun isLight() = isLight(x, y, console.settings.zapperDetectionRadius[port], console.ppu)

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
    }

    companion object {
        private inline fun isLight(mx: Int, my: Int, radius: Int, ppu: Ppu): Boolean {
            val scanline = ppu.scanline
            val cycle = ppu.cycle

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
                                    ppu.getPixelBrightness(x, y) >= 85U
                                ) {
                                    // Light cannot be detected if the Y/X position is further
                                    // ahead than the PPU, or if the PPU drew a dark color
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