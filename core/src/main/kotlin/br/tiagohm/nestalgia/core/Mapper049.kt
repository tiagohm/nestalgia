package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_049

class Mapper049(console: Console) : MMC3(console) {

    private var selectedBlock = 0
    private var prgReg = 0
    private var prgMode49 = false

    override val registerStartAddress = 0x6000

    override val registerEndAddress = 0xFFFF

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        selectedBlock = 0
        prgReg = 0
        prgMode49 = false
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, (page and 0x7F) or (selectedBlock * 0x80), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(
            slot,
            if (prgMode49) (page and 0x0F) or (selectedBlock * 0x10) else (prgReg * 4 + slot),
            memoryType,
        )
    }

    override fun writeRegister(addr: Int, value: Int) {
        if (addr < 0x8000) {
            if (canWriteToWram) {
                selectedBlock = (value shr 6) and 0x03
                prgReg = (value shr 4) and 0x03
                prgMode49 = value.bit0

                updateState()
            }
        } else {
            super.writeRegister(addr, value)
        }
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("selectedBlock", selectedBlock)
        s.write("prgReg", prgReg)
        s.write("prgMode49", prgMode49)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        selectedBlock = s.readInt("selectedBlock")
        prgReg = s.readInt("prgReg")
        prgMode49 = s.readBoolean("prgMode49")
    }
}
