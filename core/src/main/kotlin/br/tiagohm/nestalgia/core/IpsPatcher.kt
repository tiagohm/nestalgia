package br.tiagohm.nestalgia.core

object IpsPatcher {

    fun patch(
        ipsData: UByteArray,
        input: UByteArray,
        output: MutableList<UByte>,
    ): Boolean {
        // "PATCH"
        return if (ipsData[0].toInt() == 80 &&
            ipsData[1].toInt() == 65 &&
            ipsData[2].toInt() == 84 &&
            ipsData[3].toInt() == 67 &&
            ipsData[4].toInt() == 72
        ) {
            val records = ArrayList<IpsRecord>()
            var truncateOffset = -1
            var maxOutputSize = input.size
            var offset = 5

            while (true) {
                val record = IpsRecord()
                val length = record.read(Pointer(ipsData, offset))

                if (length != -1) {
                    offset += length

                    if (record.address + record.length + record.repeatCount > maxOutputSize) {
                        maxOutputSize = record.address + record.length + record.repeatCount
                    }

                    records.add(record)
                }
                // EOF, try to read truncate offset record if it exists
                else {
                    offset += 3

                    if (offset + 4 < ipsData.size) {
                        truncateOffset =
                            ipsData[offset + 2].toInt() and (ipsData[offset + 1].toInt() shl 8) and (ipsData[offset].toInt() shl 16)
                    }

                    break
                }
            }

            if (maxOutputSize >= output.size) for (i in 0 until maxOutputSize - output.size) output.add(0U)
            else for (i in 0 until output.size - maxOutputSize) output.removeLastOrNull()

            for (i in input.indices) output[i] = input[i]

            for (record in records) {
                if (record.length == 0) {
                    for (i in record.address until record.address + record.repeatCount) output[i] = record.value
                } else {
                    for (i in record.replacement.indices) output[record.address + i] = record.replacement[i]
                }
            }

            if (truncateOffset != -1 && output.size > truncateOffset) {
                for (i in 0 until truncateOffset - output.size) output.removeLastOrNull()
            }

            true
        } else {
            false
        }
    }

    fun createPatch(originalData: UByteArray, newData: UByteArray): UByteArray {
        assert(originalData.size == newData.size)

        // TODO: Não fazer isso!
        val patchFile = ArrayList<UByte>(originalData.size)

        patchFile.addAll(PATCH_BYTES)

        var i = 0
        val length = originalData.size

        while (i < length) {
            while (i < length && originalData[i] == newData[i]) {
                i++
            }

            if (i < length) {
                var rleByte = newData[i]
                var rleCount = 0
                var createRleRecord = false

                val recordAddress = i
                var recordLength = 0

                while (i < length && recordLength < 65535 && originalData[i] != newData[i]) {
                    if (newData[i] == rleByte) {
                        rleCount++
                    } else if (createRleRecord) {
                        break
                    } else {
                        rleByte = newData[i]
                        rleCount = 1
                    }

                    recordLength++
                    i++

                    if ((recordLength == rleCount && rleCount > 3) || rleCount > 13) {
                        if (recordLength == rleCount) {
                            // Same character since the start of this entry, make the RLE entry now.
                            createRleRecord = true
                        } else {
                            recordLength -= rleCount
                            i -= rleCount
                            break
                        }
                    }
                }

                val record = if (createRleRecord) {
                    IpsRecord(recordAddress, 0, repeatCount = rleCount, value = rleByte)
                } else {
                    // TODO: Optimize sliceArray using an wrapper like ByteBuffer.
                    val replacement = newData.sliceArray(recordAddress until recordAddress + recordLength)
                    IpsRecord(recordAddress, recordLength, replacement)
                }

                record.write(patchFile)
            }
        }

        patchFile.addAll(EOF_BYTES)

        return patchFile.toUByteArray()
    }

    private val PATCH_BYTES = ubyteArrayOf(80U, 65U, 84U, 67U, 72U)
    private val EOF_BYTES = ubyteArrayOf(69U, 79U, 70U)
}
