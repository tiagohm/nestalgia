package br.tiagohm.nestalgia.core

import java.io.IOException

object HeaderlessLoader {

    fun load(rom: IntArray, name: String): RomData {
        val crc = rom.crc32()
        val header = GameDatabase[crc]?.nesHeader ?: throw IOException("Invalid rom file")
        return INesLoader.load(rom, name, header)
    }
}
