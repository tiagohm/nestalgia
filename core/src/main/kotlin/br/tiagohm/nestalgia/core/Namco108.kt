package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_206

open class Namco108(console: Console) : MMC3(console) {

    override fun updateMirroring() {
        // Do nothing - Namco 108 has hardwired mirroring only
        // Mirroring is hardwired, one game uses 4-screen mirroring (Gauntlet, DRROM)
    }

    override fun writeRegister(addr: Int, value: Int) {
        //Redirect all 0x8000-0xFFFF writes to 0x8000-0x8001, all other features do not exist in this version
        val a = addr and 0x8001

        if (a == 0x8000) {
            // Disable CHR Mode 1 and PRG Mode 1
            // PRG always has the last two 8KiB banks fixed to the end
            // CHR always gives the left pattern table (0000-0FFF) the two 2KiB banks,
            // and the right pattern table (1000-1FFF) the four 1KiB banks
            super.writeRegister(a, value and 0x3F)
        } else {
            super.writeRegister(a, value)
        }
    }
}
