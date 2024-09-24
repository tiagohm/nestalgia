package br.tiagohm.nestalgia.core

import org.apache.commons.compress.archivers.sevenz.SevenZFile

object CompressedRomLoader {

    fun load(
        rom: ByteArray,
        name: String,
        fdsBios: ByteArray = ByteArray(0),
    ) = if (rom.is7z()) {
        RomLoader.load(rom.decompress7z(), name, fdsBios)
    } else {
        RomLoader.load(rom, name, fdsBios)
    }

    // https://en.wikipedia.org/wiki/List_of_file_signatures
    private val MAGIG_BYTES_7Z = byteArrayOf(0x37, 0x7A, 0xBC.toByte(), 0xAF.toByte(), 0x27, 0x1C)

    fun ByteArray.is7z(): Boolean {
        return size > 6 && (0..5).all { MAGIG_BYTES_7Z[it] == this[it] }
    }

    fun ByteArray.decompress7z(): ByteArray {
        return SevenZFile.builder()
            .setByteArray(this)
            .get()
            .use { it.getInputStream(it.nextEntry).use { s -> s.readAllBytes() } }
    }
}
