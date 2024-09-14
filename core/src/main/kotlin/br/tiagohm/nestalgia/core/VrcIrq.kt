package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL

class VrcIrq(private val console: Console) : Resetable, Clockable, Snapshotable {

    @Volatile private var irqReloadValue = 0
    @Volatile private var irqCounter = 0
    @Volatile private var irqPrescalerCounter = 0
    @Volatile private var irqEnabled = false
    @Volatile private var irqEnabledAfterAck = false
    @Volatile private var irqCycleMode = false

    override fun reset(softReset: Boolean) {
        irqReloadValue = 0
        irqCounter = 0
        irqPrescalerCounter = 0
        irqEnabled = false
        irqEnabledAfterAck = false
        irqCycleMode = false
    }

    override fun clock() {
        if (irqEnabled) {
            irqPrescalerCounter -= 3

            if (irqCycleMode || irqPrescalerCounter <= 0) {
                if (irqCounter == 0xFF) {
                    irqCounter = irqReloadValue
                    console.cpu.setIRQSource(EXTERNAL)
                } else {
                    irqCounter++
                }

                irqPrescalerCounter += 341
            }
        }
    }

    fun reloadValue(value: Int) {
        irqReloadValue = value
    }

    fun reloadValueNibble(value: Int, highBits: Boolean) {
        irqReloadValue = if (highBits) {
            (irqReloadValue and 0x0F) or (value and 0x0F shl 4)
        } else {
            (irqReloadValue and 0xF0) or (value and 0x0F)
        }
    }

    fun controlValue(value: Int) {
        irqEnabledAfterAck = value.bit0
        irqEnabled = value.bit1
        irqCycleMode = value.bit2

        if (irqEnabled) {
            irqCounter = irqReloadValue
            irqPrescalerCounter = 341
        }

        console.cpu.clearIRQSource(EXTERNAL)
    }

    fun acknowledgeIrq() {
        irqEnabled = irqEnabledAfterAck
        console.cpu.clearIRQSource(EXTERNAL)
    }

    override fun saveState(s: Snapshot) {
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqPrescalerCounter", irqPrescalerCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("irqEnabledAfterAck", irqEnabledAfterAck)
        s.write("irqCycleMode", irqCycleMode)
    }

    override fun restoreState(s: Snapshot) {
        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqPrescalerCounter = s.readInt("irqPrescalerCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        irqEnabledAfterAck = s.readBoolean("irqEnabledAfterAck")
        irqCycleMode = s.readBoolean("irqCycleMode")
    }
}
