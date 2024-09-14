package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.HORI_TRACK
import kotlin.math.max
import kotlin.math.min

// https://www.nesdev.org/wiki/Hori_Track

class HoriTrack(console: Console, keyMapping: KeyMapping) : StandardController(console, HORI_TRACK, EXP_DEVICE_PORT, keyMapping) {

    @Volatile private var horiTrackStateBuffer = 0

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (addr == 0x4016) {
            strobeOnRead()
            val output = horiTrackStateBuffer and 0x01 shl 1
            horiTrackStateBuffer = horiTrackStateBuffer shr 1
            output
        } else {
            0
        }
    }

    override fun refreshStateBuffer() {
        var dx = max(-8, min(console.keyManager.mouseX / 17 - 8, 7))
        var dy = max(-8, min(15 * console.keyManager.mouseY / 240 - 8, 7))

        dx = (dx and 0x08 shr 3) or (dx and 0x04 shr 1) or (dx and 0x02 shl 1) or (dx and 0x01 shl 3)
        dy = (dy and 0x08 shr 3) or (dy and 0x04 shr 1) or (dy and 0x02 shl 1) or (dy and 0x01 shl 3)

        val byte1 = (dy.inv() and 0x0F) or (dx.inv() and 0x0F shl 4)
        val byte2 = 0x09

        super.refreshStateBuffer()

        horiTrackStateBuffer = (stateBuffer and 0xFF) or (byte1 shl 8) or (byte2 shl 16)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("horiTrackStateBuffer", horiTrackStateBuffer)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        horiTrackStateBuffer = s.readInt("horiTrackStateBuffer")
    }
}
