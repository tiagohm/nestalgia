package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*

// https://wiki.nesdev.com/w/index.php/Four_Score

// TODO: Estender de ControllerHub.

class FourScore(console: Console) : ControlDevice(console, NONE, EXP_DEVICE_PORT) {

    private var signature4016 = 0
    private var signature4017 = 0

    override fun refreshStateBuffer() {
        // Signature for port 0 = 0x10, reversed bit order => 0x08
        // Signature for port 1 = 0x20, reversed bit order => 0x04
        signature4016 = 0x08 shl 16
        signature4017 = 0x04 shl 16
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        strobeOnRead()

        var output = 0

        when (addr) {
            0x4016 -> {
                output = signature4016 and 0x01
                signature4016 = signature4016 shr 1
            }
            0x4017 -> {
                output = signature4017 and 0x01
                signature4017 = signature4017 shr 1
            }
        }

        return output
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("signature4016", signature4016)
        s.write("signature4017", signature4017)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        signature4016 = s.readInt("signature4016")
        signature4017 = s.readInt("signature4017")
    }
}
