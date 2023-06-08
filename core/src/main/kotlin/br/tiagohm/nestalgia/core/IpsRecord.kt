package br.tiagohm.nestalgia.core

import java.io.OutputStream

internal class IpsRecord(
    @JvmField var address: Int = 0,
    @JvmField var length: Int = 0,
    @JvmField var replacement: IntArray = IntArray(0),
    @JvmField var repeatCount: Int = 0,
    @JvmField var value: Int = 0,
) {

    fun read(data: Pointer): Int {
        // EOF
        return if (data[0] == 69 &&
            data[1] == 79 &&
            data[2] == 70
        ) {
            -1
        } else {
            address = data[2] or (data[1] shl 8) or (data[0] shl 16)
            length = data[4] or (data[3] shl 8)

            return if (length == 0) {
                // RLE record
                repeatCount = data[6] or (data[5] shl 8)
                value = data[7]
                8
            } else {
                replacement = data.slice(5 until 5 + length)
                5 + length
            }
        }
    }

    fun write(output: OutputStream) {
        output.write(address shr 16 and 0xFF)
        output.write(address shr 8 and 0xFF)
        output.write(address and 0xFF)

        output.write(length shr 8 and 0xFF)
        output.write(length and 0xFF)

        if (length == 0) {
            output.write(repeatCount shr 8 and 0xFF)
            output.write(repeatCount and 0xFF)
            output.write(value)
        } else {
            replacement.forEach(output::write)
        }
    }
}
