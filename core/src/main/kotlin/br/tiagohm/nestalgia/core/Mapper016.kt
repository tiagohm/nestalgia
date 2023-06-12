package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryOperation.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_016

class Mapper016 : BandaiFgc() {

    override fun initialize() {
        super.initialize()

        // INES Mapper 016 submapper 4: FCG-1/2 ASIC, no serial EEPROM, banked CHR-ROM.
        // INES Mapper 016 submapper 5: LZ93D50 ASIC and no or 256-byte serial EEPROM, banked CHR-ROM.

        // Add a 256 byte serial EEPROM (24C02)
        if (!isNes20 || (info.subMapperId == 5 && info.header.saveRamSize == 256)) {
            // Connect a 256-byte EEPROM for iNES roms, and when submapper 5 + 256 bytes of save
            // ram in header.
            standardEeprom = Eeprom24C02(console)
        }

        if (info.subMapperId == 4) {
            removeRegisterRange(0x8000, 0xFFFF, WRITE)
        } else if (info.subMapperId == 5) {
            removeRegisterRange(0x6000, 0x7FFF, WRITE)
        }
    }
}
