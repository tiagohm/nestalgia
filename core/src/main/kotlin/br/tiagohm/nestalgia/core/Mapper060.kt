package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_060

class Mapper060(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    private var resetCounter = 0

    override fun initialize() {
        updatePage(0)
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun updatePage(page: Int) {
        selectPrgPage(0, page)
        selectPrgPage(1, page)
        selectChrPage(0, page)
    }

    override fun reset(softReset: Boolean) {
        if (softReset) {
            resetCounter = (resetCounter + 1) % 4
            updatePage(resetCounter)
        }
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
