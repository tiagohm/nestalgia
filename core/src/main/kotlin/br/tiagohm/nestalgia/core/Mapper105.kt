package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_105

class Mapper105 : MMC1() {

    private var initState = 0
    private var irqCounter = 0L
    private var irqEnabled = false

    override val dipSwitchCount = 4

    override fun init() {
        super.init()

        initState = 0
        irqCounter = 0L
        irqEnabled = false
        stateA000 = stateA000 or 0x10U // Set I bit to 1
    }

    override fun processCpuClock() {
        if (irqEnabled) {
            irqCounter++

            val maxCounter = 0x20000000 or (dipSwitches shl 25)

            if (irqCounter >= maxCounter) {
                console.cpu.setIRQSource(IRQSource.EXTERNAL)
                irqEnabled = false
            }
        }
    }

    override fun updateState() {
        if (initState == 0 && !stateA000.bit4) {
            initState = 1
        } else if (initState == 1 && stateA000.bit4) {
            initState = 2
        }

        if (stateA000.bit4) {
            irqEnabled = false
            irqCounter = 0
            console.cpu.clearIRQSource(IRQSource.EXTERNAL)
        } else {
            irqEnabled = true
        }

        mirroringType = when (state8000.toInt() and 3) {
            0 -> MirroringType.SCREEN_A_ONLY
            1 -> MirroringType.SCREEN_B_ONLY
            2 -> MirroringType.VERTICAL
            else -> MirroringType.HORIZONTAL
        }

        val access = if (stateE000.bit4) MemoryAccessType.NO_ACCESS else MemoryAccessType.READ_WRITE
        setCpuMemoryMapping(0x6000U, 0x7FFFU, 0, if (hasBattery) PrgMemoryType.SRAM else PrgMemoryType.WRAM, access)

        if (initState == 2) {
            if (stateA000.bit3) {
                // MMC1 mode
                val prgReg = ((stateE000 and 0x07U) or 0x08U).toUShort()

                if (state8000.bit3) {
                    if (state8000.bit2) {
                        selectPrgPage(0U, prgReg)
                        selectPrgPage(1U, 0x0FU)
                    } else {
                        selectPrgPage(0U, 0x08U)
                        selectPrgPage(1U, prgReg)
                    }
                } else {
                    selectPrgPage2x(0U, prgReg and 0xFEU)
                }
            } else {
                selectPrgPage2x(0U, (stateA000 and 0x06U).toUShort())
            }
        } else {
            selectPrgPage2x(0U, 0U)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("initState", initState)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        initState = s.readInt("initState") ?: 0
        irqCounter = s.readLong("irqCounter") ?: 0
        irqEnabled = s.readBoolean("irqEnabled") ?: false
    }
}