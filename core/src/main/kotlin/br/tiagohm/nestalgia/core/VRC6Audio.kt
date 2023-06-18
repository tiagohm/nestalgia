package br.tiagohm.nestalgia.core

class VRC6Audio(console: Console) : ExpansionAudio(console), Memory, Resetable {

    private var haltAudio = false
    private var lastOutput = 0

    private val pulse1 = Vrc6Pulse()
    private val pulse2 = Vrc6Pulse()
    private val saw = Vrc6Saw()

    override fun clockAudio() {
        if (!haltAudio) {
            pulse1.clock()
            pulse2.clock()
            saw.clock()
        }

        val outputLevel = pulse1.volume + pulse2.volume + saw.volume
        console.apu.addExpansionAudioDelta(AudioChannel.VRC6, outputLevel - lastOutput)
        lastOutput = outputLevel
    }

    override fun reset(softReset: Boolean) {
        lastOutput = 0
        haltAudio = false
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr) {
            0x9000, 0x9001, 0x9002 -> pulse1.write(addr, value)
            0x9003 -> {
                haltAudio = value.bit0
                val frequencyShift = if (value.bit2) 8 else if (value.bit1) 4 else 0
                pulse1.frequencyShift = frequencyShift
                pulse2.frequencyShift = frequencyShift
                saw.frequencyShift = frequencyShift
            }
            0xA000, 0xA001, 0xA002 -> pulse2.write(addr, value)
            0xB000, 0xB001, 0xB002 -> saw.write(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("haltAudio", haltAudio)
        s.write("lastOutput", lastOutput)

        s.write("pulse1", pulse1)
        s.write("pulse2", pulse2)
        s.write("saw", saw)
    }

    override fun restoreState(s: Snapshot) {
        haltAudio = s.readBoolean("haltAudio")
        lastOutput = s.readInt("lastOutput")

        s.readSnapshotable("pulse1", pulse1)
        s.readSnapshotable("pulse2", pulse2)
        s.readSnapshotable("saw", saw)
    }
}
