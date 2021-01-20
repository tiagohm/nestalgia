package br.tiagohm.nestalgia.core

import java.io.IOException

@ExperimentalUnsignedTypes
class RomLoader {

    fun load(rom: ByteArray, name: String): RomData {
        if (rom.isEmpty()) {
            throw IOException("Empty ROM")
        }

        if (rom.size < 15) {
            throw IOException("Invalid file format")
        }

        val magic = String(rom, 0, 8)

        return when {
            magic.startsWith("NES\u001a") -> INesLoader().load(rom, name)
            else -> throw IOException("Invalid file format")
        }
    }
}