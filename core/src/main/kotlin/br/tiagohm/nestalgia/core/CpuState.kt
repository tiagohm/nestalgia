package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class CpuState(
    var pc: UShort = 0U,
    var sp: UByte = 0U,
    var a: UByte = 0U,
    var x: UByte = 0U,
    var y: UByte = 0U,
    var ps: UByte = 0U,
    var irq: UInt = 0U,
    var nmi: Boolean = false,
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
        s.load()

        pc = s.readUShort("pc") ?: 0U
        sp = s.readUByte("sp") ?: 0U
        a = s.readUByte("a") ?: 0U
        x = s.readUByte("x") ?: 0U
        y = s.readUByte("y") ?: 0U
        ps = s.readUByte("ps") ?: 0U
        irq = s.readUInt("irq") ?: 0U
        nmi = s.readBoolean("nmi") ?: false
    }
}
