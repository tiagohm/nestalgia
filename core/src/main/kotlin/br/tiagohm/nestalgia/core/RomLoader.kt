package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.*

object RomLoader {

    fun load(
        rom: ByteArray,
        name: String,
        fdsBios: ByteArray = ByteArray(0),
    ): RomData {
        if (rom.isEmpty()) {
            throw IOException("Empty ROM")
        }

        if (rom.size < 15) {
            throw IOException("Invalid file format")
        }

        val data = when {
            rom.startsWith("NES\u001A") -> INesLoader.load(rom, name)
            rom.startsWith("UNIF") -> UnifLoader.load(rom, name)
            rom.startsWith("FDS\u001A") ||
                    rom.startsWith("\u0001*NINTENDO-HVC*") -> FdsLoader.load(rom, name, fdsBios)
            else -> HeaderlessLoader.load(rom, name)
        }

        val system = if (data.info.system == GameSystem.UNKNOWN) {
            // Use filename to detect PAL/VS system games
            name.toLowerCase(Locale.ENGLISH).let {
                if (it.contains("(e)") ||
                    it.contains("(australia)") ||
                    it.contains("(europe)") ||
                    it.contains("(germany)") ||
                    it.contains("(spain)")
                ) GameSystem.PAL
                else if (it.contains("(vs)")) GameSystem.VS_SYSTEM
                else GameSystem.NTSC
            }
        } else {
            data.info.system
        }

        return data.copy(info = data.info.copy(system = system))
    }
}