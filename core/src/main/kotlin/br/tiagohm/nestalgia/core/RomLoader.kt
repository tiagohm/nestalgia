package br.tiagohm.nestalgia.core

import java.io.IOException

@ExperimentalUnsignedTypes
class RomLoader {

    fun load(data: ByteArray, name: String): RomData {
        if (data.isEmpty()) {
            throw IOException("Empty ROM")
        }

        if (data.size < 15) {
            throw IOException("Invalid file format")
        }

        val magic = String(data, 0, 8)

        return when {
            magic.startsWith("NES\u001a") -> INesLoader().load(data, name)
            else -> throw IOException("Invalid file format")
        }
    }
}