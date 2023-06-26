package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_157

class Mapper157(console: Console) : BandaiFgc(console) {

    private var barcodeReader: DatachBarcodeReader? = null

    override fun initialize() {
        super.initialize()

        // Mapper 157 is used for Datach Joint ROM System boards.
        barcodeReader = DatachBarcodeReader(console)
        console.controlManager.addSystemControlDevice(barcodeReader!!)

        // Datach Joint ROM System.
        // It contains an internal 256-byte serial EEPROM (24C02)
        // that is shared among all Datach games.
        // One game, Battle Rush: Build up Robot Tournament, has an additional
        // external 128-byte serial EEPROM (24C01) on the game cartridge.
        // The NES 2.0 header's PRG-NVRAM field will only denote whether
        // the game cartridge has an additional 128-byte serial EEPROM.
        if (!isNes20 || info.header.saveRamSize == 128) {
            extraEeprom = Eeprom24C01(console)
        }

        // All mapper 157 games have an internal 256-byte EEPROM.
        standardEeprom = Eeprom24C02(console)

        // Mappers 157 and 159 do not need to support the FCG-1 and -2 and so should
        // only mirror the ports across $8000-$FFFF.
        removeRegisterRange(0x6000, 0x7FFF, WRITE)
    }

    override fun readRegister(addr: Int): Int {
        return super.readRegister(addr) or barcodeReader!!.value
    }
}
