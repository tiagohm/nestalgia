package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.GameInputType.*
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.StandardController.Button.*
import br.tiagohm.nestalgia.core.VsSystemType.*

// https://www.nesdev.org/wiki/Vs._System

class VsControlManager(console: Console) : ControlManager(console) {

    private var refreshState = false
    private var vsSystemType = DEFAULT
    private var protectionCounter = 0
    @JvmField internal var prgChrSelectBit = 0

    private val input = VsInputButtons(console)

    init {
        addSystemControlDevice(input)
    }

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        // Unsure about this, needed for VS Wrecking Crew.
        // Tiago: Dual-System only?
        // updateMainSubBit(0x00)

        vsSystemType = console.mapper!!.info.vsType
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        super.memoryRanges(ranges)

        ranges.addHandler(READ, 0x4020, 0x5FFF)
        ranges.addHandler(WRITE, 0x4020, 0x5FFF)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var value = 0

        when (addr) {
            0x4016 -> {
                val dipSwitches = console.settings.dipSwitches

                value = super.read(addr, type) and 0x65
                if (dipSwitches.bit0) value = value or 0x08
                if (dipSwitches.bit1) value = value or 0x10
            }
            0x4017 -> {
                value = super.read(addr, type) and 0x01

                val dipSwitches = console.settings.dipSwitches

                if (dipSwitches.bit2) value = value or 0x04
                if (dipSwitches.bit3) value = value or 0x08
                if (dipSwitches.bit4) value = value or 0x10
                if (dipSwitches.bit5) value = value or 0x20
                if (dipSwitches.bit6) value = value or 0x40
                if (dipSwitches.bit7) value = value or 0x80
            }
            0x5E00 -> protectionCounter = 0
            0x5E01 -> if (vsSystemType == TKO_BOXING_PROTECTION) {
                return PROTECTION_DATA[0][protectionCounter++ and 0x1F]
            } else if (vsSystemType == RBI_BASEBALL_PROTECTION) {
                return PROTECTION_DATA[1][protectionCounter++ and 0x1F]
            }
            else -> if (vsSystemType == SUPER_XEVIOUS_PROTECTION) {
                return PROTECTION_DATA[2][protectionCounter++ and 0x1F]
            }
        }

        return value
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        super.write(addr, value, type)

        refreshState = value.bit0

        if (addr == 0x4016) {
            prgChrSelectBit = value shr 2 and 0x01
        }
    }

    override fun remapControllerButtons() {
        val device1 = controlDevice(0) ?: return
        val device2 = controlDevice(1) ?: return

        val inputType = console.mapper!!.info.inputType

        if (inputType == VS_SYSTEM_SWAPPED) {
            // Swap controllers 1 & 2.
            // But don't swap the start/select buttons.
            ControlDevice.swapButtons(device1, UP, device2, UP)
            ControlDevice.swapButtons(device1, DOWN, device2, DOWN)
            ControlDevice.swapButtons(device1, LEFT, device2, LEFT)
            ControlDevice.swapButtons(device1, RIGHT, device2, RIGHT)
            ControlDevice.swapButtons(device1, B, device2, B)
            ControlDevice.swapButtons(device1, A, device2, A)
            ControlDevice.swapButtons(device1, TURBO_A, device2, TURBO_A)
            ControlDevice.swapButtons(device1, TURBO_B, device2, TURBO_B)
        } else if (inputType == VS_SYSTEM_SWAP_AB) {
            // Swap buttons P1 A & P2 B (Pinball (Japan)).
            ControlDevice.swapButtons(device1, B, device2, A)
        }

        // Swap Start/Select for all configurations (makes it more intuitive).
        ControlDevice.swapButtons(device1, START, device1, SELECT)
        ControlDevice.swapButtons(device2, START, device2, SELECT)

        if (vsSystemType == RAID_ON_BUNGELING_BAY_PROTECTION || vsSystemType == ICE_CLIMBER_PROTECTION) {
            // Bit 3 of the input status must always be on.
            device1.setBit(START)
            device2.setBit(START)
        }
    }

    override fun openBusMask(port: Int) = 0

    fun insertCoin(port: Int) {
        input.insertCoin(port)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("prgChrSelectBit", prgChrSelectBit)
        s.write("protectionCounter", protectionCounter)
        s.write("refreshState", refreshState)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        prgChrSelectBit = s.readInt("prgChrSelectBit")
        protectionCounter = s.readInt("protectionCounter")
        refreshState = s.readBoolean("refreshState")
    }

    companion object {

        private val PROTECTION_DATA = arrayOf(
            intArrayOf(
                0xFF, 0xBF, 0xB7, 0x97, 0x97, 0x17, 0x57, 0x4F,
                0x6F, 0x6B, 0xEB, 0xA9, 0xB1, 0x90, 0x94, 0x14,
                0x56, 0x4E, 0x6F, 0x6B, 0xEB, 0xA9, 0xB1, 0x90,
                0xD4, 0x5C, 0x3E, 0x26, 0x87, 0x83, 0x13, 0x00,
            ),
            intArrayOf(
                0x00, 0x00, 0x00, 0x00, 0xB4, 0x00, 0x00, 0x00,
                0x00, 0x6F, 0x00, 0x00, 0x00, 0x00, 0x94, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            ),
            intArrayOf(
                0x05, 0x01, 0x89, 0x37, 0x05, 0x00, 0xD1, 0x3E,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00,
            ),
        )
    }
}
