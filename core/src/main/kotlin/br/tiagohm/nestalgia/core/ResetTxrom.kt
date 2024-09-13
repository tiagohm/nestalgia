package br.tiagohm.nestalgia.core

// https://www.nesdev.org/wiki/NES_2.0_Mapper_313

class ResetTxrom(console: Console) : MMC3(console) {

    @Volatile private var resetCounter = 0

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            resetCounter = resetCounter + 1 and 0x03
            updateState()
        } else {
            resetCounter = 0
        }
    }

    override fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType) {
        super.selectChrPage(slot, resetCounter shl 7 or (page and 0x7F), memoryType)
    }

    override fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType) {
        super.selectPrgPage(slot, resetCounter shl 4 or (page and 0x0F), memoryType)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("resetCounter", resetCounter)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        resetCounter = s.readInt("resetCounter")
    }
}
