package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_070
// https://wiki.nesdev.com/w/index.php/INES_Mapper_152

class Bandai74161And7432(console: Console, private var enableMirroringControl: Boolean) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override fun initialize() {
        selectPrgPage(0, 0)
        selectPrgPage(1, -1)
        selectChrPage(0, 0)

        // Hack to make Kamen Rider Club - Gekitotsu Shocker Land work correctly (bad header)
        mirroringType = VERTICAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        val mirroringBit = value.bit7

        if (mirroringBit) {
            // If any game tries to set the bit to true, assume it will use mirroring switches
            // This is a hack to make as many games as possible work without CRC checks.
            enableMirroringControl = true
        }

        if (enableMirroringControl) {
            mirroringType = if (mirroringBit) SCREEN_B_ONLY else SCREEN_A_ONLY
        }

        // Biggest PRG ROM I could find for mapper 70/152 is 128kb,
        // so the 4th bit will never be used on those.
        selectPrgPage(0, value shr 4 and 0x07)
        selectChrPage(0, value and 0x0F)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("enableMirroringControl", enableMirroringControl)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        enableMirroringControl = s.readBoolean("enableMirroringControl")
    }
}
