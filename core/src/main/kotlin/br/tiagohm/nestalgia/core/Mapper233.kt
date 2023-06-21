package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/INES_Mapper_233

class Mapper233(console: Console) : Mapper226(console) {

    private var reset = 0

    override fun reset(softReset: Boolean) {
        super.reset(softReset)

        if (softReset) {
            reset = reset xor 0x01
            updatePrg()
        } else {
            reset = 0
        }
    }

    override fun prgPage(): Int {
        return registers[0] and 0x1F or (reset shl 5) or (registers[1] and 0x01 shl 6)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("reset", reset)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        reset = s.readInt("reset")
    }
}
