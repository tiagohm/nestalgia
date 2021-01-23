package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class IpsRecord(
    var address: Int = 0,
    var length: Int = 0,
    var replacement: UByteArray = UByteArray(0),
    var repeatCount: Int = 0,
    var value: UByte = 0U,
) {

    fun read(data: Pointer): Int {
        // EOF
        return if (data[0].toInt() == 69 &&
            data[1].toInt() == 79 &&
            data[2].toInt() == 70
        ) {
            -1
        } else {
            address = data[2].toInt() + (data[1].toInt() shl 8) + (data[0].toInt() shl 16)
            length = data[4].toInt() + (data[3].toInt() shl 8)

            return if (length == 0) {
                // RLE record
                repeatCount = data[6].toInt() + (data[5].toInt() shl 8)
                value = data[7]
                8
            } else {
                replacement = data.slice(5 until 5 + length)
                5 + length
            }
        }
    }

    fun write(output: MutableList<UByte>) {
        output.add((address shr 16).toUByte())
        output.add((address shr 8).toUByte())
        output.add(address.toUByte())

        output.add((length shr 8).toUByte())
        output.add((length).toUByte())

        if (length == 0) {
            output.add((repeatCount shr 8).toUByte())
            output.add((repeatCount).toUByte())
            output.add(value)
        } else {
            output.addAll(replacement)
        }
    }
}