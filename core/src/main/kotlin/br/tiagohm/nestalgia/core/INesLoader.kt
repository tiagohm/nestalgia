package br.tiagohm.nestalgia.core

import java.io.IOException

object INesLoader {

    fun load(rom: ByteArray, name: String, preloadedHeader: NesHeader? = null): RomData {
        var dataSize = rom.size.toUInt()
        var offset = 0

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

        val romCrc32 = rom.crc32(offset until rom.size)
        val romMd5 = rom.md5(offset until rom.size)
        val romSha1 = rom.sha1(offset until rom.size)
        val romSha256 = rom.sha256(offset until rom.size)
        val romCrcHex = String.format("%08X", romCrc32)

        val db = GameDatabase.get(romCrc32)
        val prgSize: UInt
        val chrSize: UInt

        if (db != null) {
            System.err.println("$name: $romCrcHex")
            prgSize = db.prgRomSize.toUInt()
            chrSize = db.chrRomSize.toUInt()
        } else {
            System.err.println("The game $name ($romCrcHex) is not in database")
            prgSize = header.prgSize
            chrSize = header.chrSize
        }

        if (prgSize + chrSize > dataSize) {
            System.err.println("WARNING: File length does not match header information")
        } else if (prgSize + chrSize < dataSize) {
            System.err.println("WARNING: File is larger than excepted")
        }

        val prgRom = UByteArray(prgSize.toInt()) { i -> if (offset + i < rom.size) rom[offset + i].toUByte() else 0U }
        val prgCrc32 = rom.crc32(offset until offset + prgSize.toInt())
        val prgMd5 = rom.md5(offset until offset + prgSize.toInt())
        val prgSha1 = rom.sha1(offset until offset + prgSize.toInt())
        val prgSha256 = rom.sha256(offset until offset + prgSize.toInt())

        offset += prgSize.toInt()

        val chrRom = UByteArray(chrSize.toInt()) { i -> if (offset + i < rom.size) rom[offset + i].toUByte() else 0U }
        val chrCrc32 = rom.crc32(offset until offset + chrSize.toInt())
        val chrMd5 = rom.md5(offset until offset + chrSize.toInt())
        val chrSha1 = rom.sha1(offset until offset + chrSize.toInt())
        val chrSha256 = rom.sha256(offset until offset + chrSize.toInt())

        val hash = HashInfo(
            prgCrc32,
            prgMd5,
            prgSha1,
            prgSha256,
            chrCrc32,
            chrMd5,
            chrSha1,
            chrSha256,
            romCrc32,
            romMd5,
            romSha1,
            romSha256,
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
            "",
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