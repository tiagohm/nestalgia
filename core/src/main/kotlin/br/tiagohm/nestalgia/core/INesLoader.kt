package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.zip.CRC32

@ExperimentalUnsignedTypes
object INesLoader {

    fun load(rom: ByteArray, name: String, preloadedHeader: NesHeader? = null): RomData {
        var dataSize = rom.size.toUInt()
        var offset = 0

        val romCrc = CRC32().let { it.update(rom); it.value }

        val header = if (preloadedHeader != null) {
            preloadedHeader
        } else {
            offset = 16
            dataSize -= 16U
            NesHeader(
                String(rom, 0, 4),
                rom[4].toUByte(),
                rom[5].toUByte(),
                rom[6].toUByte(),
                rom[7].toUByte(),
                rom[8].toUByte(),
                rom[9].toUByte(),
                rom[10].toUByte(),
                rom[11].toUByte(),
                rom[12].toUByte(),
                rom[13].toUByte(),
                rom[14].toUByte(),
                rom[15].toUByte(),
            )
        }

        val treinerData = if (header.hasTrainer) {
            if (dataSize >= 512U) {
                val bytes = UByteArray(512) { i -> rom[offset + i].toUByte() }
                offset += 512
                dataSize -= 512U
                bytes
            } else {
                throw IOException("File length does not match header information")
            }
        } else {
            UByteArray(0)
        }

        val prgChrRomCrc = CRC32().let { it.update(rom, offset, rom.size - offset); it.value }

        val db = GameDatabase.get(prgChrRomCrc)
        val prgSize: UInt
        val chrSize: UInt

        if (db != null) {
            System.err.println(db)
            prgSize = db.prgRomSize.toUInt()
            chrSize = db.chrRomSize.toUInt()
        } else {
            System.err.println("The game $name is not in database")
            prgSize = header.prgSize
            chrSize = header.chrSize
        }

        if (prgSize + chrSize > dataSize) {
            System.err.println("WARNING: File length does not match header information")
        } else if (prgSize + chrSize < dataSize) {
            System.err.println("WARNING: File is larger than excepted")
        }

        System.err.println(String.format("Game CRC: %08X", prgChrRomCrc))

        val prgRom = UByteArray(prgSize.toInt()) { i -> rom[offset + i].toUByte() }
        val prgCrc = CRC32().let { it.update(rom, offset, prgSize.toInt()); it.value }
        System.err.println(String.format("Game PRG CRC: %08X", prgCrc))

        offset += prgSize.toInt()

        val chrRom = UByteArray(chrSize.toInt()) { i -> rom[offset + i].toUByte() }
        val chrCrc = CRC32().let { it.update(rom, offset, chrSize.toInt()); it.value }
        System.err.println(String.format("Game CHR CRC: %08X", chrCrc))

        val hash = HashInfo(
            romCrc,
            prgCrc,
            chrCrc,
            prgChrRomCrc,
            "",
            "",
        )

        val info = RomInfo(
            name,
            RomFormat.INES,
            header.romHeaderVersion == RomHeaderVersion.NES20,
            false,
            0,
            header.mapperId,
            header.subMapperId,
            header.system,
            header.vsSystemType,
            header.inputType,
            header.vsPpuModel,
            false,
            header.hasBattery,
            header.hasTrainer,
            header.mirroringType,
            BusConflictType.DEFAULT,
            hash,
            header,
            null,
            db,
        )

        val data = RomData(
            info,
            header.chrRamSize,
            header.saveChrRamSize,
            header.saveRamSize,
            header.workRamSize,
            prgRom,
            chrRom,
            treinerData,
            bytes = rom,
        )

        return db?.update(data, preloadedHeader != null) ?: data
    }
}