package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.A12StateChange.RISE
import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_116

class Mapper116(console: Console) : Mapper(console) {

    private val a12Watcher = A12Watcher()
    @Volatile private var mode = 0

    private val vrc2Chr = IntArray(8)
    private val vrc2Prg = IntArray(2)
    @Volatile private var vrc2Mirroring = 0

    private val mmc3Regs = IntArray(10)
    @Volatile private var mmc3Ctrl = 0
    @Volatile private var mmc3Mirroring = 0

    private val mmc1Regs = IntArray(4)
    @Volatile private var mmc1Buffer = 0
    @Volatile private var mmc1Shift = 0

    @Volatile private var irqCounter = 0
    @Volatile private var irqReloadValue = 0
    @Volatile private var irqReload = false
    @Volatile private var irqEnabled = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x400

    override val registerStartAddress = 0x4100

    override val registerEndAddress = 0xFFFF

    override fun initialize() {
        vrc2Chr[0] = -1
        vrc2Chr[1] = -1
        vrc2Chr[2] = -1
        vrc2Chr[3] = -1
        vrc2Chr[4] = 4
        vrc2Chr[5] = 5
        vrc2Chr[6] = 6
        vrc2Chr[7] = 7
        vrc2Prg[0] = 0
        vrc2Prg[1] = 1

        mmc3Regs[0] = 0
        mmc3Regs[1] = 2
        mmc3Regs[2] = 4
        mmc3Regs[3] = 5
        mmc3Regs[4] = 6
        mmc3Regs[5] = 7
        mmc3Regs[6] = -4
        mmc3Regs[7] = -3
        mmc3Regs[8] = -2
        mmc3Regs[9] = -1

        mmc1Regs[0] = 0xC

        updateState()
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (mode and 0x03 == 1) {
            if (a12Watcher.updateVRAMAddress(addr, console.ppu.frameCycle) == RISE) {
                if (irqCounter == 0 || irqReload) {
                    irqCounter = irqReloadValue
                } else {
                    irqCounter--
                }

                if (irqCounter == 0 && irqEnabled) {
                    console.cpu.setIRQSource(EXTERNAL)
                }

                irqReload = false
            }
        }
    }

    private fun updatePrg() {
        when (mode and 0x03) {
            0 -> {
                selectPrgPage(0, vrc2Prg[0])
                selectPrgPage(1, vrc2Prg[1])
                selectPrgPage(2, -2)
                selectPrgPage(3, -1)
            }
            1 -> {
                val prgMode = mmc3Ctrl shr 5 and 0x02
                selectPrgPage(0, mmc3Regs[6 + prgMode])
                selectPrgPage(1, mmc3Regs[7])
                selectPrgPage(2, mmc3Regs[6 + (prgMode xor 0x02)])
                selectPrgPage(3, mmc3Regs[9])
            }
            2, 3 -> {
                val bank = mmc1Regs[3] and 0x0F

                if (mmc1Regs[0].bit3) {
                    if (mmc1Regs[0].bit2) {
                        selectPrgPage2x(0, bank shl 1)
                        selectPrgPage2x(1, 0x0F shl 1)
                    } else {
                        selectPrgPage2x(0, 0)
                        selectPrgPage2x(1, bank shl 1)
                    }
                } else {
                    selectPrgPage4x(0, bank and 0xFE shl 1)
                }
            }
        }
    }

    private fun updateChr() {
        val outerBank = mode and 0x04 shl 6

        when (mode and 0x03) {
            0 -> repeat(8) { selectChrPage(it, outerBank or vrc2Chr[it]) }
            1 -> {
                val slotSwap = if (mmc3Ctrl.bit7) 4 else 0
                selectChrPage(0 xor slotSwap, outerBank or (mmc3Regs[0] and 0xFE))
                selectChrPage(1 xor slotSwap, outerBank or (mmc3Regs[0] or 1))
                selectChrPage(2 xor slotSwap, outerBank or (mmc3Regs[1] and 0xFE))
                selectChrPage(3 xor slotSwap, outerBank or (mmc3Regs[1] or 1))
                selectChrPage(4 xor slotSwap, outerBank or mmc3Regs[2])
                selectChrPage(5 xor slotSwap, outerBank or mmc3Regs[3])
                selectChrPage(6 xor slotSwap, outerBank or mmc3Regs[4])
                selectChrPage(7 xor slotSwap, outerBank or mmc3Regs[5])
            }
            2, 3 -> {
                if (mmc1Regs[0].bit4) {
                    selectChrPage4x(0, mmc1Regs[1] shl 2)
                    selectChrPage4x(1, mmc1Regs[2] shl 2)
                } else {
                    selectChrPage8x(0, mmc1Regs[1] and 0xFE shl 2)
                }
            }
        }
    }

    private fun updateMirroring() {
        when (mode and 0x03) {
            0 -> mirroringType = if (vrc2Mirroring.bit0) HORIZONTAL else VERTICAL
            1 -> mirroringType = if (mmc3Mirroring.bit0) HORIZONTAL else VERTICAL
            2, 3 -> when (mmc1Regs[0] and 0x03) {
                0 -> mirroringType = SCREEN_A_ONLY
                1 -> mirroringType = SCREEN_B_ONLY
                2 -> mirroringType = VERTICAL
                3 -> mirroringType = HORIZONTAL
            }
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateState() {
        updatePrg()
        updateChr()
        updateMirroring()
    }

    private fun writeVrc2Register(addr: Int, value: Int) {
        if (addr in 0xB000..0xE003) {
            val regIndex = (addr and 0x02 or (addr shr 10) shr 1) + 2 and 0x07
            val lowHighNibble = addr and 1 shl 2
            vrc2Chr[regIndex] = vrc2Chr[regIndex] and (0xF0 shr lowHighNibble) or (value and 0x0F shl lowHighNibble)
            updateChr()
        } else {
            when (addr and 0xF000) {
                0x8000 -> {
                    vrc2Prg[0] = value
                    updatePrg()
                }
                0xA000 -> {
                    vrc2Prg[1] = value
                    updatePrg()
                }
                0x9000 -> {
                    vrc2Mirroring = value
                    updateMirroring()
                }
            }
        }
    }

    private fun writeMmc3Register(addr: Int, value: Int) {
        when (addr and 0xE001) {
            0x8000 -> {
                mmc3Ctrl = value
                updateState()
            }
            0x8001 -> {
                mmc3Regs[mmc3Ctrl and 0x07] = value
                updateState()
            }
            0xA000 -> {
                mmc3Mirroring = value
                updateState()
            }
            0xC000 -> irqReloadValue = value
            0xC001 -> irqReload = true
            0xE000 -> {
                console.cpu.clearIRQSource(EXTERNAL)
                irqEnabled = false
            }
            0xE001 -> irqEnabled = true
        }
    }

    private fun writeMmc1Register(addr: Int, value: Int) {
        if (value.bit7) {
            mmc1Regs[0] = mmc1Regs[0] or 0xc
            mmc1Shift = 0
            mmc1Buffer = 0
            updateState()
        } else {
            val regIndex = (addr shr 13) - 4
            mmc1Buffer = mmc1Buffer or (value and 0x01 shl mmc1Shift++)

            if (mmc1Shift == 5) {
                mmc1Regs[regIndex] = mmc1Buffer
                mmc1Shift = 0
                mmc1Buffer = 0
                updateState()
            }
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (addr and 0x4100 == 0x4100) {
                mode = value

                if (addr.bit0) {
                    mmc1Regs[0] = 0xc
                    mmc1Regs[3] = 0
                    mmc1Buffer = 0
                    mmc1Shift = 0
                }

                updateState()
            }
        } else {
            when (mode and 0x03) {
                0 -> writeVrc2Register(addr, value)
                1 -> writeMmc3Register(addr, value)
                2, 3 -> writeMmc1Register(addr, value)
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("a12Watcher", a12Watcher)
        s.write("mode", mode)

        s.write("vrc2Chr", vrc2Chr)
        s.write("vrc2Prg", vrc2Prg)
        s.write("vrc2Mirroring", vrc2Mirroring)

        s.write("mmc3Regs", mmc3Regs)
        s.write("mmc3Ctrl", mmc3Ctrl)
        s.write("mmc3Mirroring", mmc3Mirroring)

        s.write("mmc1Regs", mmc1Regs)
        s.write("mmc1Buffer", mmc1Buffer)
        s.write("mmc1Shift", mmc1Shift)

        s.write("irqCounter", irqCounter)
        s.write("irqReloadValue", irqReloadValue)
        s.write("irqReload", irqReload)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("a12Watcher", a12Watcher)
        mode = s.readInt("mode")

        s.readIntArray("vrc2Chr", vrc2Chr)
        s.readIntArray("vrc2Prg", vrc2Prg)
        vrc2Mirroring = s.readInt("vrc2Mirroring")

        s.readIntArray("mmc3Regs", mmc3Regs)
        mmc3Ctrl = s.readInt("mmc3Ctrl")
        mmc3Mirroring = s.readInt("mmc3Mirroring")

        s.readIntArray("mmc1Regs", mmc1Regs)
        mmc1Buffer = s.readInt("mmc1Buffer")
        mmc1Shift = s.readInt("mmc1Shift")

        irqCounter = s.readInt("irqCounter")
        irqReloadValue = s.readInt("irqReloadValue")
        irqReload = s.readBoolean("irqReload")
        irqEnabled = s.readBoolean("irqEnabled")
    }
}
