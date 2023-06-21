package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_182

class Mapper182(console: Console) : MMC3(console) {

    override fun writeRegister(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8001 -> super.writeRegister(0xA000, value)
            0xA000 -> {
                var data = value and 0xF8

                when (value and 0x07) {
                    0 -> data = data or 0
                    1 -> data = data or 3
                    2 -> data = data or 1
                    3 -> data = data or 5
                    4 -> data = data or 6
                    5 -> data = data or 7
                    6 -> data = data or 2
                    7 -> data = data or 4
                }

                super.writeRegister(0x8000, data)
            }
            0xC000 -> super.writeRegister(0x8001, value)
            0xC001 -> {
                super.writeRegister(0xC000, value)
                super.writeRegister(0xC001, value)
            }
            0xE000 -> super.writeRegister(0xE000, value)
            0xE001 -> super.writeRegister(0xE001, value)
        }
    }
}
