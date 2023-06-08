package br.tiagohm.nestalgia.core

import java.io.ByteArrayOutputStream
import java.nio.IntBuffer

object IpsPatcher {

    @JvmStatic private val PATCH_BYTES = "PATCH".toByteArray(Charsets.US_ASCII)
    @JvmStatic private val EOF_BYTES = "EOF".toByteArray(Charsets.US_ASCII)

    @JvmStatic
    fun patch(
        ipsData: IntArray,
        input: IntArray,
    ): IntArray {
        // "PATCH"
        return if (ipsData[0] == 80 &&
            ipsData[1] == 65 &&
            ipsData[2] == 84 &&
            ipsData[3] == 67 &&
            ipsData[4] == 72
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
                // EOF, try to read truncate offset record if it exists.
                else {
                    offset += 3

                    if (offset + 4 < ipsData.size) {
                        truncateOffset = ipsData[offset + 2] or (ipsData[offset + 1] shl 8) or (ipsData[offset] shl 16)
                    }

                    break
                }
            }

            val output = IntBuffer.allocate(maxOutputSize)
            output.put(input)

            for (record in records) {
                if (record.length == 0) {
                    for (i in record.address until record.address + record.repeatCount) {
                        output.put(i, record.value)
                    }
                } else {
                    for (i in record.replacement.indices) {
                        output.put(record.address + i, record.replacement[i])
                    }
                }
            }

            output.flip()

            if (truncateOffset != -1 && output.position() > truncateOffset) {
                val size = truncateOffset - output.position()
                IntArray(size).also(output::get)
            } else {
                IntArray(output.remaining()).also(output::get)
            }
        } else {
            IntArray(0)
        }
    }

    fun create(originalData: IntArray, newData: IntArray): IntArray {
        val patchFile = object : ByteArrayOutputStream(originalData.size) {

            fun toIntArray() = buf.toIntArray(count)
        }

        patchFile.write(PATCH_BYTES)

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

        patchFile.writeBytes(EOF_BYTES)

        return patchFile.toIntArray()
    }
}
