package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Zapper

@Suppress("NOTHING_TO_INLINE")
class Zapper(console: Console, port: Int) : ControlDevice(console, port) {

    @JvmField
    var x = 0

    @JvmField
    var y = 0

    override fun setStateFromInput() {
        if (console.keyManager == null) return

        if (console.settings.isInputEnabled && console.keyManager!!.isMouseButtonPressed(MouseButton.LEFT)) {
            setBit(ZapperButton.FIRE)
        }

        if (console.keyManager!!.isMouseButtonPressed(MouseButton.RIGHT)) {
            x = 0
            y = 0
        } else {
            x = console.keyManager!!.x
            y = console.keyManager!!.y
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        var output: UByte = 0U

        if ((isExpansionDevice && addr.toUInt() == 0x4017U) || isCurrentPort(addr)) {
            output = ((if (isLight()) 0x00U else 0x08U) or
                    (if (isPressed(ZapperButton.FIRE)) 0x10U else 0x00U)).toUByte()
        }

        return output
    }

    fun isLight() = isLight(x, y, console.settings.zapperDetectionRadius[port], console.ppu)

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("x", x)
        s.write("y", y)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        x = s.readInt("x") ?: x
        y = s.readInt("y") ?: y
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