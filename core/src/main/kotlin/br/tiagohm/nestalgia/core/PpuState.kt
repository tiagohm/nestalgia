package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class PpuState(
    var control: UByte = 0U,
    var mask: UByte = 0U,
    var status: UByte = 0U,
    var spriteRamAddr: UInt = 0U,
    var videoRamAddr: UShort = 0U,
    var xScroll: UByte = 0U,
    var tmpVideoRamAddr: UShort = 0U,
    var writeToggle: Boolean = false,
    var highBitShift: UShort = 0U,
    var lowBitShift: UShort = 0U,
) : Snapshotable {

    override fun saveState(s: Snapshot) {
        s.write("control", control)
        s.write("mask", mask)
        s.write("status", status)
        s.write("spriteRamAddr", spriteRamAddr)
        s.write("videoRamAddr", videoRamAddr)
        s.write("xScroll", xScroll)
        s.write("tmpVideoRamAddr", tmpVideoRamAddr)
        s.write("writeToggle", writeToggle)
        s.write("highBitShift", highBitShift)
        s.write("lowBitShift", lowBitShift)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        control = s.readUByte("control") ?: 0U
        mask = s.readUByte("mask") ?: 0U
        status = s.readUByte("status") ?: 0U
        spriteRamAddr = s.readUInt("spriteRamAddr") ?: 0U
        videoRamAddr = s.readUShort("videoRamAddr") ?: 0U
        xScroll = s.readUByte("xScroll") ?: 0U
        tmpVideoRamAddr = s.readUShort("tmpVideoRamAddr") ?: 0U
        writeToggle = s.readBoolean("writeToggle") ?: false
        highBitShift = s.readUShort("highBitShift") ?: 0U
        lowBitShift = s.readUShort("lowBitShift") ?: 0U
    }
}