package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.BARCODE_BATTLER

// https://en.wikipedia.org/wiki/Barcode_Battler

class BarcodeBattlerReader(console: Console) : ControlDevice(console, BARCODE_BATTLER, EXP_DEVICE_PORT), BarcodeReader {

    @Volatile private var newBarcode = 0L
    @Volatile private var newBarcodeDigitCount = 0
    @Volatile private var insertCycle = 0L
    @Volatile private var barcodeText = ""

    private val barcodeStream = IntArray(STREAM_SIZE)

    override fun inputBarcode(barcode: Long, digitCount: Int) {
        newBarcode = barcode
        newBarcodeDigitCount = digitCount
    }

    override fun setStateFromInput() {
        barcodeText = ""

        if (newBarcodeDigitCount > 0) {
            // Pad 8 or 13 character barcode with 0s at start.
            val text = "$newBarcode".padStart(newBarcodeDigitCount, '0')

            newBarcode = 0
            newBarcodeDigitCount = 0
            barcodeText = text
        }
    }

    override fun onAfterSetState() {
        if (barcodeText.isNotEmpty()) {
            initBarcodeStream()
            insertCycle = console.cpu.cycleCount
        }
    }

    private fun initBarcodeStream() {
        // Signature at the end, needed for code to be recognized
        barcodeText += "EPOCH\u000D\u000A"
        // Pad to 20 characters with spaces
        barcodeText = barcodeText.padStart(20, ' ')

        var pos = 0

        repeat(20) {
            barcodeStream[pos++] = 1

            for (j in 0..7) {
                barcodeStream[pos++] = (barcodeText[it].code shr j and 0x01).inv()
            }

            barcodeStream[pos++] = 0
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (addr == 0x4017) {
            val elapsedCycles = console.cpu.cycleCount - insertCycle
            val streamPosition = (elapsedCycles / CYCLES_PER_BIT).toInt()

            if (streamPosition < STREAM_SIZE) {
                return barcodeStream[streamPosition] shl 2
            }
        }

        return 0
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("newBarcode", newBarcode)
        s.write("newBarcodeDigitCount", newBarcodeDigitCount)
        s.write("insertCycle", insertCycle)
        s.write("barcodeStream", barcodeStream)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        newBarcode = s.readLong("newBarcode")
        newBarcodeDigitCount = s.readInt("newBarcodeDigitCount")
        insertCycle = s.readLong("insertCycle")
        s.readIntArray("barcodeStream", barcodeStream)
    }

    companion object {

        private const val STREAM_SIZE = 200
        private const val CYCLES_PER_BIT = Cpu.CLOCK_RATE_NTSC / 1200
    }
}
