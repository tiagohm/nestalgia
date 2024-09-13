package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory
import java.io.IOException

object INesLoader {

    private val LOG = LoggerFactory.getLogger(INesLoader::class.java)

    fun load(rom: IntArray, name: String, preloadedHeader: NesHeader? = null): RomData {
        var dataSize = rom.size
        var offset = 0

        val header = if (preloadedHeader != null) {
            preloadedHeader
        } else {
            offset = 16
            dataSize -= 16

            NesHeader(
                String(rom, 0, 4),
                rom[4],
                rom[5],
                rom[6],
                rom[7],
                rom[8],
                rom[9],
                rom[10],
                rom[11],
                rom[12],
                rom[13],
                rom[14],
                rom[15],
            )
        }

        val treinerData = if (header.hasTrainer) {
            if (dataSize >= 512) {
                val bytes = IntArray(512) { i -> rom[offset + i] }
                offset += 512
                dataSize -= 512
                bytes
            } else {
                throw IOException("File length does not match header information")
            }
        } else {
            IntArray(0)
        }

        val romCrc32 = rom.crc32(offset until rom.size)
        val romMd5 = rom.md5(offset until rom.size)
        val romSha1 = rom.sha1(offset until rom.size)
        val romSha256 = rom.sha256(offset until rom.size)
        val romCrcHex = String.format("%08X", romCrc32)

        val db = GameDatabase[romCrc32]
        val prgSize: Int
        val chrSize: Int

        if (db != null) {
            LOG.info("the game $name ($romCrcHex) was found in database. info={}", db)
            prgSize = db.prgRomSize
            chrSize = db.chrRomSize
        } else {
            LOG.info("the game $name ($romCrcHex) is not in database")
            prgSize = header.prgSize
            chrSize = header.chrSize
        }

        if (prgSize + chrSize > dataSize) {
            LOG.warn("file length does not match header information. {} > {}", prgSize + chrSize, dataSize)
        } else if (prgSize + chrSize < dataSize) {
            LOG.warn("file is larger than excepted. {} < {}", prgSize + chrSize, dataSize)
        }

        val prgRom = IntArray(prgSize) { i -> if (offset + i < rom.size) rom[offset + i] else 0 }
        val prgCrc32 = rom.crc32(offset until offset + prgSize)
        val prgMd5 = rom.md5(offset until offset + prgSize)
        val prgSha1 = rom.sha1(offset until offset + prgSize)
        val prgSha256 = rom.sha256(offset until offset + prgSize)

        offset += prgSize

        val chrRom = IntArray(chrSize) { i -> if (offset + i < rom.size) rom[offset + i] else 0 }
        val chrCrc32 = rom.crc32(offset until offset + chrSize)
        val chrMd5 = rom.md5(offset until offset + chrSize)
        val chrSha1 = rom.sha1(offset until offset + chrSize)
        val chrSha256 = rom.sha256(offset until offset + chrSize)

        val hash = HashInfo(
            prgCrc32, prgMd5, prgSha1, prgSha256,
            chrCrc32, chrMd5, chrSha1, chrSha256,
            romCrc32, romMd5, romSha1, romSha256,
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
            rawData = rom,
        )

        return db?.update(data, preloadedHeader != null) ?: data
    }
}
