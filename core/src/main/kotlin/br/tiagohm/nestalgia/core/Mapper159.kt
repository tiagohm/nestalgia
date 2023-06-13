package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_159

class Mapper159(console: Console) : BandaiFgc(console) {

    override fun initialize() {
        super.initialize()

        // LZ93D50 with 128 byte serial EEPROM (24C01).
        standardEeprom = Eeprom24C01(console)

        // Mappers 157 and 159 do not need to support the FCG-1 and -2 and so should
        // only mirror the ports across $8000-$FFFF.
        removeRegisterRange(0x6000, 0x7FFF, WRITE)
    }
}
