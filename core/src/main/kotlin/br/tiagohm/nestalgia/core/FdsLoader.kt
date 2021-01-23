package br.tiagohm.nestalgia.core

import java.io.IOException
import java.util.zip.CRC32

// https://wiki.nesdev.com/w/index.php/FDS_file_format
// https://wiki.nesdev.com/w/index.php/FDS_disk_format

@ExperimentalUnsignedTypes
object FdsLoader {

    fun load(rom: ByteArray, name: String, bios: ByteArray): RomData {
        if (bios.isEmpty()) {
            throw IOException("BIOS is empty")
        }

        val romCrc = CRC32().let { it.update(rom); it.value }

        val info = RomInfo(
            name,
            RomFormat.FDS,
            mapperId = Mapper.FDS_MAPPER_ID,
            mirroring = MirroringType.VERTICAL,
            system = GameSystem.FDS,
            hash = HashInfo(romCrc, romCrc)
        )

        return RomData(
            info,
            prgRom = bios.toUByteArray(),
            bytes = rom,
            biosMissing = bios.size != 0x2000,
            fdsBios = bios,
        )
    }

    private fun addGaps(diskSide: MutableList<UByte>, buffer: Pointer) {
        // Start image with 28300 bits of gap
        for (i in 0 until 28300 / 8) diskSide.add(0U)

        var j = 0

        while (j < FDS_DISK_SIDE_CAPACITY) {
            val blockType = buffer[j].toInt()

            val blockLength = when (blockType) {
                1 -> 56 // Disk header
                2 -> 2 // File count
                3 -> 16 // File header
                4 -> 1 + buffer[j - 3].toInt() + buffer[j - 2].toInt() * 0x100
                else -> 1
            }

            if (blockType == 0) {
                diskSide.add(blockType.toUByte())
            } else {
                diskSide.add(0x80U)

                for (k in j until j + blockLength) {
                    if (k in buffer) {
                        diskSide.add(buffer[k])
                    }
                }

                // Fake CRC value
                diskSide.add(0x4DU)
                diskSide.add(0x62U)

                // Insert 976 bits of gap after a block
                for (i in 0 until 976 / 8) diskSide.add(0U)
            }

            j += blockLength
        }
    }

    fun loadDiskData(
        data: UByteArray,
        diskSides: MutableList<UByteArray>,
        diskHeaders: MutableList<UByteArray>,
    ) {
        var offset = 0

        val hasHeader = data[0].toInt() == 70 &&
                data[1].toInt() == 68 &&
                data[2].toInt() == 83 &&
                data[3].toInt() == 0x1A

        val numberOfSides = if (hasHeader) {
            offset = 16
            data[4].toInt()
        } else {
            data.size / FDS_DISK_SIDE_CAPACITY
        }

        for (i in 0 until numberOfSides) {
            diskHeaders.add(data.sliceArray(offset until offset + 56))

            val fdsDiskImage = ArrayList<UByte>(FDS_DISK_SIDE_CAPACITY)
            addGaps(fdsDiskImage, Pointer(data, offset))

            offset += FDS_DISK_SIDE_CAPACITY

            // Ensure the image is 65500 bytes
            while (fdsDiskImage.size < FDS_DISK_SIDE_CAPACITY) {
                fdsDiskImage.add(0U)
            }

            diskSides.add(fdsDiskImage.toUByteArray())
        }
    }

    fun rebuildFdsFile(
        diskSides: List<UByteArray>,
        needHeader: Boolean,
    ): UByteArray {
        val output = ArrayList<UByte>(diskSides.size * FDS_DISK_SIDE_CAPACITY + 16)

        if (needHeader) {
            val header = ubyteArrayOf(
                70U, 68U, 83U, 0x1AU, diskSides.size.toUByte(), 0U,
                0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U, 0U
            )

            output.addAll(header)
        }

        for (diskSide in diskSides) {
            var inGap = true
            var i = 0
            val length = diskSide.size
            var gapNeeded = FDS_DISK_SIDE_CAPACITY
            var fileSize = 0

            while (i < length) {
                if (inGap) {
                    if (diskSide[i].toInt() == 0x80) {
                        inGap = false
                    }

                    i++
                } else {
                    val blockLength = when (diskSide[i].toInt()) {
                        1 -> 56
                        2 -> 2
                        3 -> {
                            fileSize = (diskSide[i + 13] + diskSide[i + 14] * 0x100U).toInt()
                            16
                        }
                        4 -> 1 + fileSize
                        else -> 1
                    }

                    for (k in i until i + blockLength) output.add(diskSide[k])

                    gapNeeded -= blockLength
                    i += blockLength
                    i += 2 // Skip CRC after block

                    inGap = true
                }
            }

            while (gapNeeded-- > 0) output.add(0U)
        }

        return output.toUByteArray()
    }

    const val FDS_DISK_SIDE_CAPACITY = 65500
}