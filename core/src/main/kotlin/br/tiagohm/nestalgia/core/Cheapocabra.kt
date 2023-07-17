package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_111
// Único jogo "Ninja Ryukenden (China)" e não roda (tela verde).

class Cheapocabra(console: Console) : FlashSST39SF040Mapper(console) {

    override val prgPageSize = 0x8000

    override val chrPageSize = 0x2000

    override val workRamSize = 0

    override val saveRamSize = 0

    override val registerStartAddress = 0x5000

    override val registerEndAddress = 0x5FFF

    override val chrRamSize = 0x4000

    override val allowRegisterRead = true

    private var prgReg = 0

    override lateinit var orgPrgRom: IntArray

    override fun initialize() {
        super.initialize()

        addRegisterRange(0x7000, 0x7FFF, WRITE)

        addRegisterRange(0x8000, 0xFFFF, READ_WRITE)
        removeRegisterRange(0x5000, 0x5FFF, READ)

        writeRegister(0x5000, powerOnByte())

        orgPrgRom = prgRom.copyOf()

        applySaveData()
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            prgReg = value and 0x0F

            selectPrgPage(0, prgReg)
            selectChrPage(0, value shr 4 and 0x01)

            // TODO: Mesen itera até 8, mas causa erro de "Invalid PPU address range".
            repeat(4) {
                nametable(it, (if (value.bit5) 8 else 0) + it)
            }
        } else {
            flash.write((prgReg shl 15) or (addr and 0x7FFF), value)
        }
    }
}
