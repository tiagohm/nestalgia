package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*
import java.nio.IntBuffer

class DatachBarcodeReader(console: Console) : ControlDevice(console, DATACH_BARCODE_READER, MAPPER_INPUT_PORT), BarcodeReader {

    private var insertCycle = 0L
    private var newBarcode = 0L
    private var newBarcodeDigitCount = 0
    private val data = IntArray(1024)
    private val buffer = IntBuffer.wrap(data)
    private var barcodeText = ""

    val value: Int
        get() {
            val elapsedCycles = console.masterClock - insertCycle
            val bitNumber = (elapsedCycles / 1000).toInt()
            return if (bitNumber < buffer.position()) buffer[bitNumber] else 0
        }

    override fun setStateFromInput() {
        if (newBarcodeDigitCount > 0) {
            var text = "$newBarcode"
            // Pad 8 or 13 character barcode with 0s at start.
            text = text.padStart(newBarcodeDigitCount - barcodeText.length, '0')

            newBarcode = 0
            newBarcodeDigitCount = 0
            barcodeText = text
        }
    }

    override fun inputBarcode(barcode: Long, digitCount: Int) {
        newBarcode = barcode
        newBarcodeDigitCount = digitCount
    }

    override fun onAfterSetState() {
        if (barcodeText.isNotEmpty()) {
            initBarcodeData()
            barcodeText = ""
        }
    }

    private fun initBarcodeData() {
        insertCycle = console.masterClock

        buffer.clear()

        repeat(33) { buffer.put(8) }

        buffer.put(0)
        buffer.put(8)
        buffer.put(0)

        var sum = 0
        val code = IntArray(barcodeText.length) { barcodeText[it].code - 48 }

        if (code.size == 13) {
            for (i in 0..5) {
                val odd = PREFIX_PARITY_TYPE[code[0]][i] != 0

                for (j in 0..6) {
                    buffer.put(if (odd) DATA_LEFT_ODD[code[i + 1]][j] else DATA_LEFT_EVEN[code[i + 1]][j])
                }
            }

            buffer.put(8)
            buffer.put(0)
            buffer.put(8)
            buffer.put(0)
            buffer.put(8)

            for (i in 7..11) {
                repeat(7) { buffer.put(DATA_RIGHT[code[i]][it]) }
            }

            for (i in 0..11) {
                sum += if (i.bit0) code[i] * 3 else code[i] * 1
            }
        } else {
            for (i in 0..3) {
                repeat(7) { buffer.put(DATA_LEFT_ODD[code[i]][it]) }
            }

            buffer.put(8)
            buffer.put(0)
            buffer.put(8)
            buffer.put(0)
            buffer.put(8)

            for (i in 4..6) {
                repeat(7) { buffer.put(DATA_RIGHT[code[i]][it]) }
            }

            repeat(7) { sum += if (it.bit0) code[it] * 1 else code[it] * 3 }
        }

        sum = (10 - sum % 10) % 10

        repeat(7) { buffer.put(DATA_RIGHT[sum][it]) }

        buffer.put(0)
        buffer.put(8)
        buffer.put(0)

        repeat(32) { buffer.put(8) }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("insertCycle", insertCycle)
        s.write("newBarcode", newBarcode)
        s.write("newBarcodeDigitCount", newBarcodeDigitCount)
        s.write("data", data)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        insertCycle = s.readLong("insertCycle")
        newBarcode = s.readLong("newBarcode")
        newBarcodeDigitCount = s.readInt("newBarcodeDigitCount")
        s.readIntArrayOrFill("data", data, 0)
    }

    companion object {

        @JvmStatic private val PREFIX_PARITY_TYPE = arrayOf(
            intArrayOf(8, 8, 8, 8, 8, 8), intArrayOf(8, 8, 0, 8, 0, 0),
            intArrayOf(8, 8, 0, 0, 8, 0), intArrayOf(8, 8, 0, 0, 0, 8),
            intArrayOf(8, 0, 8, 8, 0, 0), intArrayOf(8, 0, 0, 8, 8, 0),
            intArrayOf(8, 0, 0, 0, 8, 8), intArrayOf(8, 0, 8, 0, 8, 0),
            intArrayOf(8, 0, 8, 0, 0, 8), intArrayOf(8, 0, 0, 8, 0, 8),
        )

        @JvmStatic private val DATA_LEFT_ODD = arrayOf(
            intArrayOf(8, 8, 8, 0, 0, 8, 0), intArrayOf(8, 8, 0, 0, 8, 8, 0),
            intArrayOf(8, 8, 0, 8, 8, 0, 0), intArrayOf(8, 0, 0, 0, 0, 8, 0),
            intArrayOf(8, 0, 8, 8, 8, 0, 0), intArrayOf(8, 0, 0, 8, 8, 8, 0),
            intArrayOf(8, 0, 8, 0, 0, 0, 0), intArrayOf(8, 0, 0, 0, 8, 0, 0),
            intArrayOf(8, 0, 0, 8, 0, 0, 0), intArrayOf(8, 8, 8, 0, 8, 0, 0),
        )

        @JvmStatic private val DATA_LEFT_EVEN = arrayOf(
            intArrayOf(8, 0, 8, 8, 0, 0, 0), intArrayOf(8, 0, 0, 8, 8, 0, 0),
            intArrayOf(8, 8, 0, 0, 8, 0, 0), intArrayOf(8, 0, 8, 8, 8, 8, 0),
            intArrayOf(8, 8, 0, 0, 0, 8, 0), intArrayOf(8, 0, 0, 0, 8, 8, 0),
            intArrayOf(8, 8, 8, 8, 0, 8, 0), intArrayOf(8, 8, 0, 8, 8, 8, 0),
            intArrayOf(8, 8, 8, 0, 8, 8, 0), intArrayOf(8, 8, 0, 8, 0, 0, 0),
        )

        @JvmStatic private val DATA_RIGHT = arrayOf(
            intArrayOf(0, 0, 0, 8, 8, 0, 8), intArrayOf(0, 0, 8, 8, 0, 0, 8),
            intArrayOf(0, 0, 8, 0, 0, 8, 8), intArrayOf(0, 8, 8, 8, 8, 0, 8),
            intArrayOf(0, 8, 0, 0, 0, 8, 8), intArrayOf(0, 8, 8, 0, 0, 0, 8),
            intArrayOf(0, 8, 0, 8, 8, 8, 8), intArrayOf(0, 8, 8, 8, 0, 8, 8),
            intArrayOf(0, 8, 8, 0, 8, 8, 8), intArrayOf(0, 0, 0, 8, 0, 8, 8),
        )
    }
}
