package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.GameSystem.FDS
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL
import java.nio.IntBuffer

// https://wiki.nesdev.com/w/index.php/FDS_file_format
// https://wiki.nesdev.com/w/index.php/FDS_disk_format

object FdsLoader {

    fun load(rom: IntArray, name: String, bios: IntArray): RomData {
        require(bios.isNotEmpty()) { "BIOS is empty" }

        val info = RomInfo(
            name,
            RomFormat.FDS,
            mapperId = MapperFactory.FDS_MAPPER_ID,
            mirroring = VERTICAL,
            system = FDS,
            hash = HashInfo(crc32 = rom.crc32(), md5 = rom.md5(), sha1 = rom.sha1(), sha256 = rom.sha256()),
        )

        return RomData(
            info,
            prgRom = bios,
            rawData = rom,
            biosMissing = bios.size != 0x2000,
            fdsBios = bios,
        )
    }

    private fun addGaps(diskSide: IntBuffer, buffer: Pointer) {
        // Start image with 28300 bits of gap
        for (i in 0 until 28300 / 8) diskSide.put(0)

        var j = 0

        while (j < FDS_DISK_SIDE_CAPACITY) {
            val blockType = buffer[j]

            val blockLength = when (blockType) {
                1 -> 56 // Disk header
                2 -> 2 // File count
                3 -> 16 // File header
                4 -> 1 + buffer[j - 3] + buffer[j - 2] * 0x100
                else -> 1
            }

            if (blockType == 0) {
                diskSide.put(blockType)
            } else {
                diskSide.put(0x80)

                for (k in j until j + blockLength) {
                    if (k in buffer) {
                        diskSide.put(buffer[k])
                    }
                }

                // Fake CRC value
                diskSide.put(0x4D)
                diskSide.put(0x62)

                // Insert 976 bits of gap after a block
                for (i in 0 until 976 / 8) diskSide.put(0)
            }

            j += blockLength
        }
    }

    fun loadDiskData(
        data: IntArray,
        diskSides: MutableList<IntArray>,
        diskHeaders: MutableList<IntArray>,
    ) {
        var offset = 0

        val hasHeader = data[0] == 70 &&
            data[1] == 68 &&
            data[2] == 83 &&
            data[3] == 0x1A

        val numberOfSides = if (hasHeader) {
            offset = 16
            data[4]
        } else {
            data.size / FDS_DISK_SIDE_CAPACITY
        }

        for (i in 0 until numberOfSides) {
            diskHeaders.add(data.sliceArray(offset until offset + 56))

            val fdsDiskImage = IntBuffer.allocate(FDS_DISK_SIDE_CAPACITY)
            addGaps(fdsDiskImage, Pointer(data, offset))

            offset += FDS_DISK_SIDE_CAPACITY

            // Ensure the image is 65500 bytes
            // while (fdsDiskImage.size < FDS_DISK_SIDE_CAPACITY) {
            //     fdsDiskImage.add(0)
            // }

            diskSides.add(fdsDiskImage.array())
        }
    }

    fun rebuildFdsFile(
        diskSides: List<IntArray>,
        needHeader: Boolean,
    ): IntArray {
        val output = IntBuffer.allocate(diskSides.size * FDS_DISK_SIDE_CAPACITY + 16)

        if (needHeader) {
            val header = intArrayOf(70, 68, 83, 0x1A, diskSides.size, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)
            output.put(header)
        }

        for (diskSide in diskSides) {
            var inGap = true
            var i = 0
            val length = diskSide.size
            var gapNeeded = FDS_DISK_SIDE_CAPACITY
            var fileSize = 0

            while (i < length) {
                if (inGap) {
                    if (diskSide[i] == 0x80) {
                        inGap = false
                    }

                    i++
                } else {
                    val blockLength = when (diskSide[i]) {
                        1 -> 56
                        2 -> 2
                        3 -> {
                            fileSize = diskSide[i + 13] + diskSide[i + 14] * 0x100
                            16
                        }
                        4 -> 1 + fileSize
                        else -> 1
                    }

                    for (k in i until i + blockLength) output.put(diskSide[k])

                    gapNeeded -= blockLength
                    i += blockLength
                    i += 2 // Skip CRC after block

                    inGap = true
                }
            }

            // while (gapNeeded-- > 0) output.put(0)
        }

        return output.array()
    }

    const val FDS_DISK_SIDE_CAPACITY = 65500
}
