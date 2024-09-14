package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.FOUR_PLAYER_ADAPTER
import br.tiagohm.nestalgia.core.ControllerType.FOUR_SCORE

// https://wiki.nesdev.com/w/index.php/Four_Score

class FourScore(console: Console, type: ControllerType, port: Int, vararg controllers: ControllerSettings) :
    ControllerHub(4, console, type, port, *controllers) {

    private val signature = IntArray(2)
    private val counter = IntArray(2)

    override fun refreshStateBuffer() {
        // Signature for port 0 = 0x10, reversed bit order => 0x08
        // Signature for port 1 = 0x20, reversed bit order => 0x04
        if (type == FOUR_SCORE) {
            signature[0] = 0x08
            signature[1] = 0x04
        } else {
            // Signature is reversed for Hori 4p adapter.
            signature[0] = 0x04
            signature[1] = 0x08
        }

        counter[0] = 16
        counter[1] = 16
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        strobeOnRead()

        var output = 0
        var i = addr - 0x4016

        if (counter[i] > 0) {
            counter[i]--

            if (counter[i] < 8) {
                i += 2
            }

            val device = controlDevice(i)

            if (device != null) {
                output = device.read(0x4016)
            }
        } else {
            output = signature[i] and 0x01
            signature[i] = signature[i] shr 1 or 0x80
        }

        output = output and 0x01

        if (this.type == FOUR_PLAYER_ADAPTER) {
            output = output shl 1
        }

        return output
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("signature", signature)
        s.write("counter", counter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("signature", signature)
        s.readIntArray("counter", counter)
    }
}
