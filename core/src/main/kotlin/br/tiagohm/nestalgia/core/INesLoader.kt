package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.zip.CRC32

@ExperimentalUnsignedTypes
class INesLoader {

    fun load(data: ByteArray, name: String, preloadedHeader: NesHeader? = null): RomData {
        var dataSize = data.size.toUInt()
        var offset = 0

        val romCrc = CRC32().let { it.update(data); it.value }

        val header = if (preloadedHeader != null) {
            preloadedHeader
        } else {
            offset = 16
            dataSize -= 16U
            NesHeader(
                String(data, 0, 4),
                data[4].toUByte(),
                data[5].toUByte(),
                data[6].toUByte(),
                data[7].toUByte(),
                data[8].toUByte(),
                data[9].toUByte(),
                data[10].toUByte(),
                data[11].toUByte(),
                data[12].toUByte(),
                data[13].toUByte(),
                data[14].toUByte(),
                data[15].toUByte(),
            )
        }

        val treinerData = if (header.hasTrainer) {
            if (dataSize >= 512U) {
                val bytes = UByteArray(512) { i -> data[offset + i].toUByte() }
                offset += 512
                dataSize -= 512U
                bytes
            } else {
                throw IOException("File length does not match header information")
            }
        } else {
            UByteArray(0)
        }

        val prgChrRomCrc = CRC32().let { it.update(data, offset, data.size - offset); it.value }

        val db = GameDatabase.entries[prgChrRomCrc]
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

        val prgRom = UByteArray(prgSize.toInt()) { i -> data[offset + i].toUByte() }
        val prgCrc = CRC32().let { it.update(data, offset, prgSize.toInt()); it.value }
        System.err.println(String.format("Game PRG CRC: %08X", prgCrc))

        offset += prgSize.toInt()

        val chrRom = UByteArray(chrSize.toInt()) { i -> data[offset + i].toUByte() }
        val chrCrc = CRC32().let { it.update(data, offset, chrSize.toInt()); it.value }
        System.err.println(String.format("Game CHR CRC: %08X", chrCrc))

        val hash = HashInfo(
            romCrc,
            prgCrc,
            chrCrc,
            prgChrRomCrc,
            "",
            "",
        )

        val romInfo = RomInfo(
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

        val romData = RomData(
            romInfo,
            header.chrRamSize,
            header.saveChrRamSize,
            header.saveRamSize,
            header.workRamSize,
            prgRom,
            chrRom,
            treinerData,
            null,
            null,
            null,
            data,
            false,
        )

        return db?.update(romData, preloadedHeader != null) ?: romData
    }
}