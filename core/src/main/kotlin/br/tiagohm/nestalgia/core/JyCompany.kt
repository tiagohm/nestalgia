package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.IRQSource.EXTERNAL
import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM

// https://wiki.nesdev.com/w/index.php/INES_Mapper_090
// https://wiki.nesdev.com/w/index.php/INES_Mapper_209
// https://wiki.nesdev.com/w/index.php/INES_Mapper_211
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_281
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_282
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_295
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_358
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_386
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_387
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_388
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_397
// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_421

class JyCompany(console: Console) : Mapper(console) {

    enum class JyIrqSource {
        CPU_CLOCK,
        PPU_A12_RISE,
        PPU_READ,
        CPU_WRITE
    }

    private val prgRegs = IntArray(4)
    private val chrLowRegs = IntArray(8)
    private val chrHighRegs = IntArray(8)
    private val chrLatch = IntArray(2)

    @Volatile private var prgMode = 0
    @Volatile private var enablePrgAt6000 = false
    @Volatile private var prgBlock = 0

    @Volatile private var chrMode = 0
    @Volatile private var chrBlockMode = false
    @Volatile private var chrBlock = 0
    @Volatile private var mirrorChr = false

    @Volatile private var mirroringReg = 0
    @Volatile private var advancedNtControl = false
    @Volatile private var disableNtRam = false

    @Volatile private var ntRamSelectBit = 0
    private val ntLowRegs = IntArray(4)
    private val ntHighRegs = IntArray(4)

    @Volatile private var irqEnabled = false
    @Volatile private var irqSource = JyIrqSource.CPU_CLOCK
    @Volatile private var irqCountDirection = 0
    @Volatile private var irqFunkyMode = false
    @Volatile private var irqFunkyModeReg = 0
    @Volatile private var irqSmallPrescaler = false
    @Volatile private var irqPrescaler = 0
    @Volatile private var irqCounter = 0
    @Volatile private var irqXorReg = 0

    @Volatile private var multiplyValue1 = 0
    @Volatile private var multiplyValue2 = 0
    @Volatile private var regRamValue = 0

    @Volatile private var lastPpuAddr = 0

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x0400

    override val allowRegisterRead = true

    override fun initialize() {
        removeRegisterRange(0x8000, 0xFFFF, READ)
        addRegisterRange(0x5000, 0x5FFF, READ_WRITE)

        chrLatch[0] = 0
        chrLatch[1] = 4

        updateState()
    }

    private fun updateState() {
        updatePrgState()
        updateChrState()
        updateMirroringState()
    }

    private fun invertPrgBits(prgReg: Int, needInvert: Boolean): Int {
        return if (needInvert) {
            (prgReg and 0x01 shl 6) or (prgReg and 0x02 shl 4) or (prgReg and 0x04 shl 2) or (prgReg and 0x08) or (prgReg and 0x10 shr 2) or (prgReg and 0x20 shr 4) or (prgReg and 0x40 shr 6)
        } else {
            prgReg
        }
    }

    private fun updatePrgState() {
        val invertBits = prgMode and 0x03 == 0x03

        val prgRegs = intArrayOf(
            invertPrgBits(prgRegs[0], invertBits), invertPrgBits(prgRegs[1], invertBits),
            invertPrgBits(prgRegs[2], invertBits), invertPrgBits(prgRegs[3], invertBits),
        )

        when (prgMode and 0x03) {
            0 -> {
                selectPrgPage4x(0, if (prgMode.bit2) prgRegs[3] else 0x3C)

                if (enablePrgAt6000) {
                    addCpuMemoryMapping(0x6000, 0x7FFF, prgRegs[3] * 4 + 3, ROM)
                }
            }
            1 -> {
                selectPrgPage2x(0, prgRegs[1] shl 1)
                selectPrgPage2x(1, if (prgMode.bit2) prgRegs[3] else 0x3E)

                if (enablePrgAt6000) {
                    addCpuMemoryMapping(0x6000, 0x7FFF, prgRegs[3] * 2 + 1, ROM)
                }
            }
            2, 3 -> {
                selectPrgPage(0, prgRegs[0] or (prgBlock shl 5))
                selectPrgPage(1, prgRegs[1] or (prgBlock shl 5))
                selectPrgPage(2, prgRegs[2] or (prgBlock shl 5))
                selectPrgPage(3, if (prgMode.bit2) prgRegs[3] or (prgBlock shl 5) else 0x3F)

                if (enablePrgAt6000) {
                    addCpuMemoryMapping(0x6000, 0x7FFF, prgRegs[3], ROM)
                }
            }
        }

        if (!enablePrgAt6000) {
            removeCpuMemoryMapping(0x6000, 0x7FFF)
        }
    }

    private fun chrReg(index: Int): Int {
        var newIndex = index

        if (chrMode >= 2 && mirrorChr && (newIndex == 2 || newIndex == 3)) {
            newIndex -= 2
        }

        return if (chrBlockMode) {
            val maskAndShift = when (chrMode) {
                0 -> intArrayOf(0x1F, 5)
                1 -> intArrayOf(0x3F, 6)
                2 -> intArrayOf(0x7F, 7)
                3 -> intArrayOf(0xFF, 8)
                else -> intArrayOf(0x1F, 5)
            }

            chrLowRegs[newIndex] and maskAndShift[0] or (chrBlock shl maskAndShift[1])
        } else {
            chrLowRegs[newIndex] or (chrHighRegs[newIndex] shl 8)
        }
    }

    private fun updateChrState() {
        val chrRegs = intArrayOf(chrReg(0), chrReg(1), chrReg(2), chrReg(3), chrReg(4), chrReg(5), chrReg(6), chrReg(7))

        when (chrMode) {
            0 -> selectChrPage8x(0, chrRegs[0] shl 3)
            1 -> {
                selectChrPage4x(0, chrRegs[chrLatch[0]] shl 2)
                selectChrPage4x(1, chrRegs[chrLatch[1]] shl 2)
            }
            2 -> {
                selectChrPage2x(0, chrRegs[0] shl 1)
                selectChrPage2x(1, chrRegs[2] shl 1)
                selectChrPage2x(2, chrRegs[4] shl 1)
                selectChrPage2x(3, chrRegs[6] shl 1)
            }
            3 -> repeat(8) {
                selectChrPage(it, chrRegs[it])
            }
        }
    }

    private fun updateMirroringState() {
        // Mapper 211 behaves as though N were always set (1), and mapper 090 behaves as though N were always clear(0).
        if ((advancedNtControl || info.mapperId == 211) && info.mapperId != 90) {
            repeat(4) {
                nametable(it, ntLowRegs[it] and 0x01)
            }
        } else {
            when (mirroringReg) {
                0 -> mirroringType = VERTICAL
                1 -> mirroringType = HORIZONTAL
                2 -> mirroringType = SCREEN_A_ONLY
                3 -> mirroringType = SCREEN_B_ONLY
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        return when (addr and 0xF803) {
            0x5000 -> 0 // Dip switches.
            0x5800 -> multiplyValue1 * multiplyValue2 and 0xFF
            0x5801 -> multiplyValue1 * multiplyValue2 shr 8 and 0xFF
            0x5803 -> regRamValue
            else -> console.memoryManager.openBus()
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            when (addr and 0xF803) {
                0x5800 -> multiplyValue1 = value
                0x5801 -> multiplyValue2 = value
                0x5803 -> regRamValue = value
            }
        } else {
            when (addr and 0xF007) {
                0x8000, 0x8001, 0x8002, 0x8003, 0x8004, 0x8005, 0x8006, 0x8007 -> prgRegs[addr and 0x03] = value and 0x7F
                0x9000, 0x9001, 0x9002, 0x9003, 0x9004, 0x9005, 0x9006, 0x9007 -> chrLowRegs[addr and 0x07] = value
                0xA000, 0xA001, 0xA002, 0xA003, 0xA004, 0xA005, 0xA006, 0xA007 -> chrHighRegs[addr and 0x07] = value
                0xB000, 0xB001, 0xB002, 0xB003 -> ntLowRegs[addr and 0x03] = value
                0xB004, 0xB005, 0xB006, 0xB007 -> ntHighRegs[addr and 0x03] = value
                0xC000 -> if (value.bit0) {
                    irqEnabled = true
                } else {
                    irqEnabled = false
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0xC001 -> {
                    irqCountDirection = value shr 6 and 0x03
                    irqFunkyMode = value.bit3
                    irqSmallPrescaler = value.bit2
                    irqSource = JyIrqSource.entries[value and 0x03]
                }
                0xC002 -> {
                    irqEnabled = false
                    console.cpu.clearIRQSource(EXTERNAL)
                }
                0xC003 -> irqEnabled = true
                0xC004 -> irqPrescaler = value xor irqXorReg
                0xC005 -> irqCounter = value xor irqXorReg
                0xC006 -> irqXorReg = value
                0xC007 -> irqFunkyModeReg = value
                0xD000 -> {
                    prgMode = value and 0x07
                    chrMode = value shr 3 and 0x03
                    advancedNtControl = value.bit5
                    disableNtRam = value.bit6
                    enablePrgAt6000 = value.bit7
                }
                0xD001 -> mirroringReg = value and 0x03
                0xD002 -> ntRamSelectBit = value and 0x80
                0xD003 -> {
                    mirrorChr = value.bit7
                    chrBlockMode = !value.bit5
                    chrBlock = (value and 0x18 shr 2) or (value and 0x01)

                    if (data.info.mapperId == 35 || data.info.mapperId == 90 || data.info.mapperId == 209 || data.info.mapperId == 211) {
                        prgBlock = value and 0x06
                    }
                }
            }
        }

        updateState()
    }

    override fun clock() {
        if (irqSource == JyIrqSource.CPU_CLOCK || (irqSource == JyIrqSource.CPU_WRITE && console.cpu.isCpuWrite)) {
            tickIrqCounter()
        }
    }

    override fun readVRAM(addr: Int, type: MemoryOperationType): Int {
        if (irqSource == JyIrqSource.PPU_READ && type == MemoryOperationType.PPU_RENDERING_READ) {
            tickIrqCounter()
        }

        if (addr >= 0x2000) {
            // This behavior only affects reads, not writes.
            // Additional info: https://forums.nesdev.com/viewtopic.php?f=3&t=17198
            if ((advancedNtControl || info.mapperId == 211) && info.mapperId != 90) {
                val ntIndex = ((addr and 0x2FFF) - 0x2000) / 0x400

                if (disableNtRam || ntLowRegs[ntIndex] and 0x80 != ntRamSelectBit and 0x80) {
                    val chrPage = ntLowRegs[ntIndex] or (ntHighRegs[ntIndex] shl 8)
                    val chrOffset = chrPage * 0x400 + (addr and 0x3FF)

                    return if (mChrRomSize > chrOffset) {
                        chrRom[chrOffset]
                    } else {
                        0
                    }
                }
            }
        }

        return super.readVRAM(addr, type)
    }

    override fun notifyVRAMAddressChange(addr: Int) {
        if (irqSource === JyIrqSource.PPU_A12_RISE
            && addr and 0x1000 != 0
            && lastPpuAddr and 0x1000 == 0
        ) {
            tickIrqCounter()
        }

        lastPpuAddr = addr

        if (info.mapperId == 209) {
            when (addr and 0x2FF8) {
                0x0FD8, 0x0FE8 -> {
                    chrLatch[addr shr 12] = addr shr 4 and (addr shr 10 and 0x04 or 0x02)
                    updateChrState()
                }
            }
        }
    }

    private fun tickIrqCounter() {
        var clockIrqCounter = false
        val mask = if (irqSmallPrescaler) 0x07 else 0xFF
        var prescaler = irqPrescaler and mask

        if (irqCountDirection == 0x01) {
            prescaler = (prescaler + 1) and 0xFF

            if (prescaler and mask == 0) {
                clockIrqCounter = true
            }
        } else if (irqCountDirection == 0x02) {
            prescaler = (prescaler - 1) and 0xFF

            if (prescaler == 0) {
                clockIrqCounter = true
            }
        }

        irqPrescaler = irqPrescaler and mask.inv() or (prescaler and mask)

        if (clockIrqCounter) {
            if (irqCountDirection == 0x01) {
                irqCounter = (irqCounter + 1) and 0xFF

                if (irqCounter == 0 && irqEnabled) {
                    console.cpu.setIRQSource(EXTERNAL)
                }
            } else if (irqCountDirection == 0x02) {
                irqCounter = (irqCounter - 1) and 0xFF

                if (irqCounter == 0xFF && irqEnabled) {
                    console.cpu.setIRQSource(EXTERNAL)
                }
            }
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgRegs", prgRegs)
        s.write("chrLowRegs", chrLowRegs)
        s.write("chrHighRegs", chrHighRegs)
        s.write("ntLowRegs", ntLowRegs)
        s.write("ntHighRegs", ntHighRegs)

        s.write("chrLatch", chrLatch)
        s.write("prgMode", prgMode)
        s.write("enablePrgAt6000", enablePrgAt6000)
        s.write("prgBlock", prgBlock)
        s.write("chrMode", chrMode)
        s.write("chrBlockMode", chrBlockMode)
        s.write("chrBlock", chrBlock)
        s.write("mirrorChr", mirrorChr)
        s.write("mirroringReg", mirroringReg)
        s.write("advancedNtControl", advancedNtControl)
        s.write("disableNtRam", disableNtRam)
        s.write("ntRamSelectBit", ntRamSelectBit)
        s.write("irqEnabled", irqEnabled)
        s.write("irqSource", irqSource)
        s.write("lastPpuAddr", lastPpuAddr)
        s.write("irqCountDirection", irqCountDirection)
        s.write("irqFunkyMode", irqFunkyMode)
        s.write("irqFunkyModeReg", irqFunkyModeReg)
        s.write("irqSmallPrescaler", irqSmallPrescaler)
        s.write("irqPrescaler", irqPrescaler)
        s.write("irqCounter", irqCounter)
        s.write("irqXorReg", irqXorReg)
        s.write("multiplyValue1", multiplyValue1)
        s.write("multiplyValue2", multiplyValue2)
        s.write("regRamValue", regRamValue)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("prgRegs", prgRegs)
        s.readIntArray("chrLowRegs", chrLowRegs)
        s.readIntArray("chrHighRegs", chrHighRegs)
        s.readIntArray("ntLowRegs", ntLowRegs)
        s.readIntArray("ntHighRegs", ntHighRegs)

        s.readIntArray("chrLatch", chrLatch)
        prgMode = s.readInt("prgMode")
        enablePrgAt6000 = s.readBoolean("enablePrgAt6000")
        prgBlock = s.readInt("prgBlock")
        chrMode = s.readInt("chrMode")
        chrBlockMode = s.readBoolean("chrBlockMode")
        chrBlock = s.readInt("chrBlock")
        mirrorChr = s.readBoolean("mirrorChr")
        mirroringReg = s.readInt("mirroringReg")
        advancedNtControl = s.readBoolean("advancedNtControl")
        disableNtRam = s.readBoolean("disableNtRam")
        ntRamSelectBit = s.readInt("ntRamSelectBit")
        irqEnabled = s.readBoolean("irqEnabled")
        irqSource = s.readEnum("irqSource", JyIrqSource.CPU_CLOCK)
        lastPpuAddr = s.readInt("lastPpuAddr")
        irqCountDirection = s.readInt("irqCountDirection")
        irqFunkyMode = s.readBoolean("irqFunkyMode")
        irqFunkyModeReg = s.readInt("irqFunkyModeReg")
        irqSmallPrescaler = s.readBoolean("irqSmallPrescaler")
        irqPrescaler = s.readInt("irqPrescaler")
        irqCounter = s.readInt("irqCounter")
        irqXorReg = s.readInt("irqXorReg")
        multiplyValue1 = s.readInt("multiplyValue1")
        multiplyValue2 = s.readInt("multiplyValue2")
        regRamValue = s.readInt("regRamValue")

        updateState()
    }
}
