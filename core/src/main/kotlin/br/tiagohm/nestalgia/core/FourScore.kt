package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Four_Score

class FourScore(console: Console) : ControlDevice(console, EXP_DEVICE_PORT) {

    private var signature4016 = 0U
    private var signature4017 = 0U

    override fun refreshStateBuffer() {
        // Signature for port 0 = 0x10, reversed bit order => 0x08
        // Signature for port 1 = 0x20, reversed bit order => 0x04
        signature4016 = 0x08U shl 16
        signature4017 = 0x04U shl 16
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        strobeOnRead()

        var output: UByte = 0U

        when (addr.toUInt()) {
            0x4016U -> {
                output = (signature4016 and 0x01U).toUByte()
                signature4016 = signature4016 shr 1
            }
            0x4017U -> {
                output = (signature4017 and 0x01U).toUByte()
                signature4017 = signature4017 shr 1
            }
        }

        return output
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("signature4016", signature4016)
        s.write("signature4017", signature4017)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        signature4016 = s.readUInt("signature4016") ?: 0U
        signature4017 = s.readUInt("signature4017") ?: 0U
    }
}
