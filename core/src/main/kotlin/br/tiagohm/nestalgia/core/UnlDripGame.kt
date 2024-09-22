package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MemoryAccessType.READ
import br.tiagohm.nestalgia.core.MemoryAccessType.READ_WRITE
import br.tiagohm.nestalgia.core.MemoryOperationType.PPU_RENDERING_READ
import br.tiagohm.nestalgia.core.MirroringType.*

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_284

class UnlDripGame(console: Console) : Mapper(console) {

    override val dipSwitchCount = 1

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x800

    override val allowRegisterRead = true

    override val registerStartAddress = 0x8000

    override val registerEndAddress = 0xFFFF

    private val audioChannels = arrayOf(UnlDripGameAudio(console), UnlDripGameAudio(console))
    private val extendedAttributes = arrayOf(IntArray(0x400), IntArray(0x400))
    @Volatile private var lowByteIrqCounter = 0
    @Volatile private var irqCounter = 0
    @Volatile private var lastNametableFetchAddr = 0
    @Volatile private var irqEnabled = false
    @Volatile private var extAttributesEnabled = false
    @Volatile private var wramWriteEnabled = false

    override fun initialize() {
        console.initializeRam(extendedAttributes[0])
        console.initializeRam(extendedAttributes[1])

        selectPrgPage(1, -1)

        addRegisterRange(0x4800, 0x5FFF, READ)
        removeRegisterRange(0x8000, 0xFFFF, READ)
    }

    override fun clock() {
        if (irqEnabled) {
            if (irqCounter > 0) {
                irqCounter--

                if (irqCounter <= 0) {
                    // While the IRQ counter is enabled, the timer is decremented once per CPU
                    // cycle. Once the timer reaches zero, the /IRQ line is set to logic 0 and the
                    // timer stops decrementing.
                    irqEnabled = false
                    console.cpu.setIRQSource(IRQSource.EXTERNAL)
                }
            }
        }

        audioChannels[0].clock()
        audioChannels[1].clock()
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updateWorkRamState() {
        addCpuMemoryMapping(0x6000, 0x7FFF, 0, PrgMemoryType.WRAM, if (wramWriteEnabled) READ_WRITE else READ)
    }

    override fun readVRAM(addr: Int, type: MemoryOperationType): Int {
        if (extAttributesEnabled && type == PPU_RENDERING_READ) {
            if (addr >= 0x2000 && (addr and 0x3FF) < 0x3C0) {
                // Nametable fetches.
                lastNametableFetchAddr = addr and 0x03FF
            } else if (addr >= 0x2000 && (addr and 0x3FF) >= 0x3C0) {
                // Attribute fetches.
                val bank = when (mirroringType) {
                    SCREEN_A_ONLY -> 0
                    SCREEN_B_ONLY -> 1
                    HORIZONTAL -> if (addr and 0x800 != 0) 1 else 0
                    VERTICAL -> if (addr and 0x400 != 0) 1 else 0
                    else -> 0
                }

                // Return a byte containing the same palette 4 times.
                // This allows the PPU to select the right palette no matter the shift value
                val value = extendedAttributes[bank][lastNametableFetchAddr and 0x3FF] and 0x03
                return (value shl 6) or (value shl 4) or (value shl 2) or value
            }
        }

        return super.readVRAM(addr, type)
    }

    override fun readRegister(addr: Int): Int {
        return when (addr and 0x5800) {
            0x4800 -> (if (dipSwitches > 0) 0x80 else 0) or 0x64
            0x5000 -> audioChannels[0].read(0x5000)
            0x5800 -> audioChannels[1].read(0x5800)
            else -> 0
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr <= 0xBFFF) {
            when (addr and 0x800F) {
                in 0x8000..0x8003 -> audioChannels[0].write(addr, value)
                in 0x8004..0x8007 -> audioChannels[1].write(addr, value)
                0x8008 -> lowByteIrqCounter = value
                0x8009 -> {
                    // Data written to the IRQ Counter Low register is buffered until writing to IRQ
                    // Counter High, at which point the composite data is written directly to the IRQ	timer.
                    irqCounter = value and 0x7F shl 8 or lowByteIrqCounter
                    irqEnabled = value.bit7

                    // Writing to the IRQ Enable register will acknowledge the interrupt and return the /IRQ signal to logic 1.
                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                }
                0x800A -> {
                    mirroringType = when (value and 0x03) {
                        0 -> VERTICAL
                        1 -> HORIZONTAL
                        2 -> SCREEN_A_ONLY
                        else -> SCREEN_B_ONLY
                    }

                    extAttributesEnabled = value.bit2
                    wramWriteEnabled = value.bit3
                    updateWorkRamState()
                }
                0x800B -> selectPrgPage(0, value and 0x0F)
                0x800C -> selectChrPage(0, value and 0x0F)
                0x800D -> selectChrPage(1, value and 0x0F)
                0x800E -> selectChrPage(2, value and 0x0F)
                0x800F -> selectChrPage(3, value and 0x0F)
            }
        } else {
            // Attribute expansion memory at $C000-$C7FF is mirrored throughout $C000-$FFFF.
            extendedAttributes[if (addr and 0x400 != 0) 1 else 0][addr and 0x3FF] = value
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("extendedAttributes1", extendedAttributes[0])
        s.write("extendedAttributes2", extendedAttributes[1])
        s.write("audioChannel1", audioChannels[0])
        s.write("audioChannel2", audioChannels[1])

        s.write("lowByteIrqCounter", lowByteIrqCounter)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("extAttributesEnabled", extAttributesEnabled)
        s.write("wramWriteEnabled", wramWriteEnabled)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readIntArray("extendedAttributes1", extendedAttributes[0])
        s.readIntArray("extendedAttributes2", extendedAttributes[1])
        s.readSnapshotable("audioChannel1", audioChannels[0])
        s.readSnapshotable("audioChannel2", audioChannels[1])

        lowByteIrqCounter = s.readInt("lowByteIrqCounter")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        extAttributesEnabled = s.readBoolean("extAttributesEnabled")
        wramWriteEnabled = s.readBoolean("wramWriteEnabled")

        updateWorkRamState()
    }
}
