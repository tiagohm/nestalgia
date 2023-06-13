package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_017

class Mapper017(console: Console) : FrontFareast(console) {

    override fun initialize() {
        selectPrgPage4x(0, -4)
    }

    override fun internalWriteRegister(addr: Int, value: Int) {
        when (addr) {
            0x4504, 0x4505, 0x4506, 0x4507 -> selectPrgPage(addr - 0x4504, value)
            0x4510, 0x4511, 0x4512, 0x4513, 0x4514, 0x4515, 0x4516, 0x4517 -> selectChrPage(addr - 0x4510, value)
        }
    }
}
