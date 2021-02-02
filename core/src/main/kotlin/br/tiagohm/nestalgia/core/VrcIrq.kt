package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class VrcIrq(val console: Console) :
    Resetable,
    Snapshotable {

    private var irqReloadValue: UByte = 0U
    private var irqCounter: UByte = 0U
    private var irqPrescalerCounter = 0
    private var irqEnabled = false
    private var irqEnabledAfterAck = false
    private var irqCycleMode = false

    override fun reset(softReset: Boolean) {
        irqReloadValue = 0U
        irqCounter = 0U
        irqPrescalerCounter = 0
        irqEnabled = false
        irqEnabledAfterAck = false
        irqCycleMode = false
    }

    fun processCpuCycle() {
        if (irqEnabled) {
            irqPrescalerCounter -= 3

            if (irqCycleMode || (irqPrescalerCounter <= 0 && !irqCycleMode)) {
                if (irqCounter.isFilled) {
                    irqCounter = irqReloadValue
                    console.cpu.setIRQSource(IRQSource.EXTERNAL)
                } else {
                    irqCounter++
                }

                irqPrescalerCounter += 341
            }
        }
    }

    fun setReloadValue(value: UByte) {
        irqReloadValue = value
    }

    fun setReloadValueNibble(value: UByte, highBits: Boolean) {
        irqReloadValue = if (highBits) {
            (irqReloadValue and 0x0FU) or ((value.toUInt() and 0x0FU) shl 4).toUByte()
        } else {
            (irqReloadValue and 0xF0U) or (value and 0x0FU)
        }
    }

    fun setControlValue(value: UByte) {
        irqEnabledAfterAck = value.bit0
        irqEnabled = value.bit1
        irqCycleMode = value.bit2

        if (irqEnabled) {
            irqCounter = irqReloadValue
            irqPrescalerCounter = 341
        }

        console.cpu.clearIRQSource(IRQSource.EXTERNAL)
    }

    fun acknowledgeIrq() {
        irqEnabled = irqEnabledAfterAck
        console.cpu.clearIRQSource(IRQSource.EXTERNAL)
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
        s.load()

        irqReloadValue = s.readUByte("irqReloadValue") ?: 0U
        irqCounter = s.readUByte("irqCounter") ?: 0U
        irqPrescalerCounter = s.readInt("irqPrescalerCounter") ?: 0
        irqEnabled = s.readBoolean("irqEnabled") ?: false
        irqEnabledAfterAck = s.readBoolean("irqEnabledAfterAck") ?: false
        irqCycleMode = s.readBoolean("irqCycleMode") ?: false
    }
}