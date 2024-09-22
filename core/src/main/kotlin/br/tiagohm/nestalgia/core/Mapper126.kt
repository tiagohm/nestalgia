package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_126

open class Mapper126(console: Console) : Mapper422(console) {

    override val chrOuterBank
        get() = (exRegs[0] shl 4 and 0x080) or (exRegs[0] shl 3 and 0x100) or (exRegs[0] shl 5 and 0x200)
}
