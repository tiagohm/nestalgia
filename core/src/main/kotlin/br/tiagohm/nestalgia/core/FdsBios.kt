package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/FDS_BIOS

class FdsBios(data: UByteArray) : Pointer(data, 0) {

    companion object {
        const val NINTENDO_FDS_FILENAME = "[BIOS] Nintendo Famicom Disk System (Japan).bin"
        const val NINTENDO_FDS_EARLY_FILENAME = "[BIOS] Nintendo Famicom Disk System (Japan) (Early).bin"
        const val SHARP_TWIN_FAMICOM_FILENAME = "[BIOS] Sharp Twin Famicom (Japan).bin"
    }
}