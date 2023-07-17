package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_188

class BandaiKaraoke(console: Console) : Mapper(console) {

    private val microphone = BandaiMicrophone(console)

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    override val hasBusConflicts = true

    override fun initialize() {
        addRegisterRange(0x6000, 0x7FFF, READ)
        removeRegisterRange(0x8000, 0xFFFF, READ)

        selectPrgPage(0, 0)
        selectPrgPage(1, 0x07)
        selectChrPage(0, 0)

        console.controlManager.addSystemControlDevice(microphone)
    }

    override fun readRegister(addr: Int): Int {
        return microphone.read(addr) or console.memoryManager.openBus(0xF8)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (value.bit4) {
            // Select internal rom.
            selectPrgPage(0, value and 0x07)
        } else {
            // Select expansion rom.
            if (mPrgSize >= 0x40000) {
                selectPrgPage(0, value and 0x07 or 0x08)
            } else {
                // Open bus for roms that don't contain the expansion rom.
                removeCpuMemoryMapping(0x8000, 0xBFFF)
            }
        }

        mirroringType = if (value.bit5) HORIZONTAL else VERTICAL
    }
}
