package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_155

class Mapper155(console: Console) : MMC1(console) {

    override fun updateState() {
        // WRAM disable bit does not exist in mapper 155
        wramDisable = false

        super.updateState()
    }
}
