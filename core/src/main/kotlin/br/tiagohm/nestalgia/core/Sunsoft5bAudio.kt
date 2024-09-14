package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.AudioChannel.*

@Suppress("FloatingPointLiteralPrecision")
class Sunsoft5bAudio(console: Console) : ExpansionAudio(console), Memory {

    private val volumeLut = IntArray(0x10)
    @Volatile private var currentRegister = 0
    private val registers = IntArray(0x10)
    @Volatile private var lastOutput = 0
    private val timer = IntArray(3)
    private val toneStep = IntArray(3)
    @Volatile private var processTick = false

    init {
        var output = 1.0

        for (i in 1..0xF) {
            // +1.5 dB 2x for every 1 step in volume
            output *= 1.1885022274370184377301224648922
            output *= 1.1885022274370184377301224648922
            volumeLut[i] = output.toInt() and 0xFF
        }
    }

    fun period(channel: Int): Int {
        return registers[channel * 2] or (registers[channel * 2 + 1] shl 8)
    }

    fun envelopePeriod(): Int {
        return registers[0x0B] or (registers[0x0C] shl 8)
    }

    fun noisePeriod(): Int {
        return registers[6]
    }

    fun volume(channel: Int): Int {
        return volumeLut[registers[8 + channel] and 0x0F]
    }

    fun isEnvelopeEnabled(channel: Int): Boolean {
        return registers[8 + channel] and 0x10 == 0x10
    }

    fun isToneEnabled(channel: Int): Boolean {
        return registers[7] shr channel and 0x01 == 0x00
    }

    fun isNoiseEnabled(channel: Int): Boolean {
        return registers[7] shr channel + 3 and 0x01 == 0x00
    }

    private fun updateChannel(channel: Int) {
        timer[channel]--

        if (timer[channel] <= 0) {
            timer[channel] = period(channel)
            toneStep[channel] = toneStep[channel] + 1 and 0x0F
        }
    }

    private fun updateOutputLevel() {
        var summedOutput = 0

        repeat(3) {
            if (isToneEnabled(it) && toneStep[it] < 0x08) {
                summedOutput += volume(it)
            }
        }

        console.apu.addExpansionAudioDelta(SUNSOFT_5B, summedOutput - lastOutput)
        lastOutput = summedOutput
    }

    override fun clockAudio() {
        if (processTick) {
            repeat(3, ::updateChannel)
            updateOutputLevel()
        }

        processTick = !processTick
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        when (addr and 0xE000) {
            0xC000 -> currentRegister = value
            0xE000 -> if (currentRegister <= 0x0F) {
                registers[currentRegister] = value
            }
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("currentRegister", currentRegister)
        s.write("registers", registers)
        s.write("lastOutput", lastOutput)
        s.write("timer", timer)
        s.write("toneStep", toneStep)
        s.write("processTick", processTick)
    }

    override fun restoreState(s: Snapshot) {
        currentRegister = s.readInt("currentRegister")
        s.readIntArray("registers", registers)
        lastOutput = s.readInt("lastOutput")
        s.readIntArray("timer", timer)
        s.readIntArray("toneStep", toneStep)
        processTick = s.readBoolean("processTick")
    }
}
