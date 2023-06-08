package br.tiagohm.nestalgia.core

data class CpuState(
    @JvmField var pc: Int = 0,
    @JvmField var sp: Int = 0,
    @JvmField var a: Int = 0,
    @JvmField var x: Int = 0,
    @JvmField var y: Int = 0,
    @JvmField var ps: Int = 0,
    @JvmField var irq: Int = 0,
    @JvmField var nmi: Boolean = false,
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("pc", pc)
        s.write("sp", sp)
        s.write("a", a)
        s.write("x", x)
        s.write("y", y)
        s.write("ps", ps)
        s.write("irq", irq)
        s.write("nmi", nmi)
    }

    override fun restoreState(s: Snapshot) {
        pc = s.readInt("pc")
        sp = s.readInt("sp")
        a = s.readInt("a")
        x = s.readInt("x")
        y = s.readInt("y")
        ps = s.readInt("ps")
        irq = s.readInt("irq")
        nmi = s.readBoolean("nmi")
    }
}
