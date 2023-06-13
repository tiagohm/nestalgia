package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Zapper

class Zapper(console: Console, type: ControllerType, port: Int) : ControlDevice(console, type, port) {

    @JvmField var x = 0
    @JvmField var y = 0

    override fun setStateFromInput() {
        if (console.keyManager == null) return

        if (console.settings.inputEnabled && console.keyManager!!.isMouseButtonPressed(MouseButton.LEFT)) {
            setBit(ZapperButton.FIRE)
        }

        if (console.keyManager!!.isMouseButtonPressed(MouseButton.RIGHT)) {
            x = 0
            y = 0
        } else {
            x = console.keyManager!!.mouseX
            y = console.keyManager!!.mouseY
        }
    }

    val light
        get() = console.ppu.isLight(x, y, console.settings.zapperDetectionRadius[port])

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if ((isExpansionDevice && addr == 0x4017) || isCurrentPort(addr)) {
            return (if (light) 0x00 else 0x08) or
                (if (isPressed(ZapperButton.FIRE)) 0x10 else 0x00)
        }

        return 0
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {}

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("x", x)
        s.write("y", y)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        x = s.readInt("x", x)
        y = s.readInt("y", y)
    }

    companion object {

        @JvmStatic
        private fun Ppu.isLight(mx: Int, my: Int, radius: Int): Boolean {
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
