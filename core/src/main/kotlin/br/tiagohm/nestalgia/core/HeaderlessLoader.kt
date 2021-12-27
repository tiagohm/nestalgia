package br.tiagohm.nestalgia.core

import java.io.IOException

object HeaderlessLoader {

    fun load(rom: ByteArray, name: String): RomData {
        val crc = rom.crc32()
        val header = GameDatabase.get(crc)?.nesHeader ?: throw IOException("Invalid rom file")
        return INesLoader.load(rom, name, header)
    }
}