package br.tiagohm.nestalgia.core

class McAcc : MMC3() {

    private var counter = 0
    private var prevAddr: UShort = 0U

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("counter", counter)
        s.write("prevAddr", prevAddr)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        counter = s.readInt("counter") ?: 0
        prevAddr = s.readUShort("prevAddr") ?: 0U
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        // Writing to $C001 resets pulse counter
        if ((addr.toInt() and 0xE001) == 0xC001) counter = 0
        super.writeRegister(addr, value)
    }

    override fun notifyVRAMAddressChange(addr: UShort) {
        if ((addr.toInt() and 0x1000) == 0 && (prevAddr.toInt() and 0x1000) != 0) {
            counter++

            if (counter == 1) {
                // Counter clocking happens once per 8 A12 cycles at first cycle
                if (irqCounter.isZero || irqReload) {
                    irqCounter = irqReloadValue
                } else {
                    irqCounter--
                }

                if (irqCounter.isZero && irqEnabled) {
                    console.cpu.setIRQSource(IRQSource.EXTERNAL)
                }

                irqReload = false
            } else if (counter == 8) {
                counter = 0
            }
        }

        prevAddr = addr
    }
}