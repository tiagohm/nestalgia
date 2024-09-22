package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/NES_2.0_Mapper_519

class Eh8813A(console: Console) : Mapper(console) {

    override val dipSwitchCount = 4

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    override val allowRegisterRead = true

    @Volatile private var alterReadAddress = false
    @Volatile private var prgBank = 0
    @Volatile private var chrBank = 0
    @Volatile private var lock = false
    @Volatile private var horizontalMirorring = false
    @Volatile private var nrom128 = false

    override fun initialize() {
        updateState()
    }

    override fun reset(softReset: Boolean) {
        lock = false
        nrom128 = false
        prgBank = 0
        chrBank = 0
        alterReadAddress = false
        horizontalMirorring = false
        updateState()
    }

    private fun updateState() {
        if (nrom128) {
            selectPrgPage(0, prgBank)
            selectPrgPage(1, prgBank)
        } else {
            selectPrgPage2x(0, prgBank and 0x1E)
        }

        selectChrPage(0, chrBank)
        mirroringType = if (horizontalMirorring) MirroringType.HORIZONTAL else MirroringType.VERTICAL
    }

    override fun readRegister(addr: Int): Int {
        var newAddr = addr

        if (alterReadAddress) {
            newAddr = (addr and 0xFFF0) + dipSwitches
        }

        return internalRead(newAddr)
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (!lock) {
            prgBank = addr and 0x3F
            alterReadAddress = addr.bit6
            nrom128 = addr.bit7
            lock = addr.bit8
            chrBank = value and 0x7F
            horizontalMirorring = value.bit7
        }

        chrBank = (chrBank and 0xFC) or (value and 0x03)

        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("alterReadAddress", alterReadAddress)
        s.write("prgBank", prgBank)
        s.write("chrBank", chrBank)
        s.write("lock", lock)
        s.write("horizontalMirorring", horizontalMirorring)
        s.write("nrom128", nrom128)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        alterReadAddress = s.readBoolean("alterReadAddress")
        prgBank = s.readInt("prgBank")
        chrBank = s.readInt("chrBank")
        lock = s.readBoolean("lock")
        horizontalMirorring = s.readBoolean("horizontalMirorring")
        nrom128 = s.readBoolean("nrom128")
    }
}
