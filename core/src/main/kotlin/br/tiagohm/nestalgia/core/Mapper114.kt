package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_114

class Mapper114(console: Console) : MMC3(console) {

    private val exReg = IntArray(2)

    override val registerStartAddress = 0x5000

    override val forceMmc3RevAIrqs = true

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        exReg[0] = 0
        exReg[1] = 0
    }

    override fun updatePrgMapping() {
        super.updatePrgMapping()

        if (exReg[0].bit7) {
            val page = exReg[0] and 0x0F shl 1
            selectPrgPage2x(0, page)
            selectPrgPage2x(1, page)
        } else {
            super.updatePrgMapping()
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            exReg[0] = value
            updatePrgMapping()
        } else {
            when (addr and 0xE001) {
                0x8001 -> super.writeRegister(0xA000, value)
                0xA000 -> {
                    super.writeRegister(0x8000, (value and 0xC0) or SECURITY[value and 0x07])
                    exReg[1] = 1
                }
                0xA001 -> irqReloadValue = value
                0xC000 -> {
                    if (exReg[1] != 0) {
                        exReg[1] = 0
                        super.writeRegister(0x8001, value)
                    }
                }
                0xC001 -> irqReload = true
                0xE000 -> {
                    console.cpu.clearIRQSource(EXTERNAL)
                    irqEnabled = false
                }
                0xE001 -> irqEnabled = true
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("exReg", exReg)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArrayOrFill("exReg", exReg, 0)
    }

    companion object {

        @JvmStatic private val SECURITY = intArrayOf(0, 3, 1, 5, 6, 7, 2, 4)
    }
}
