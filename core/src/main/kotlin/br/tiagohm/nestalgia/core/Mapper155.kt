package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_155

@ExperimentalUnsignedTypes
class Mapper155 : MMC1() {

    override fun updateState() {
        // WRAM disable bit does not exist in mapper 155
        state[3] = state[3] and 0x0FU

        super.updateState()
    }
}