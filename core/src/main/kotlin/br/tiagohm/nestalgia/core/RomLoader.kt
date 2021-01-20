package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.*

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

        val data = when {
            magic.startsWith("NES\u001a") -> INesLoader().load(rom, name)
            else -> throw IOException("Invalid file format")
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