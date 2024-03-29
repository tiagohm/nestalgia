package br.tiagohm.nestalgia.core

data class PpuState(
    @JvmField var control: Int = 0,
    @JvmField var mask: Int = 0,
    @JvmField var status: Int = 0,
    @JvmField var spriteRamAddr: Int = 0,
    @JvmField var videoRamAddr: Int = 0,
    @JvmField var xScroll: Int = 0,
    @JvmField var tmpVideoRamAddr: Int = 0,
    @JvmField var writeToggle: Boolean = false,
    @JvmField var highBitShift: Int = 0,
    @JvmField var lowBitShift: Int = 0,
) : Snapshotable, Resetable {

    override fun reset(softReset: Boolean) {
        control = 0
        mask = 0
        status = 0
        spriteRamAddr = 0
        // Is not cleared on reset, but it set to 0 on power-on.
        // videoRamAddr = 0
        xScroll = 0
        tmpVideoRamAddr = 0
        writeToggle = false
        highBitShift = 0
        lowBitShift = 0
    }

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
        control = s.readInt("control")
        mask = s.readInt("mask")
        status = s.readInt("status")
        spriteRamAddr = s.readInt("spriteRamAddr")
        videoRamAddr = s.readInt("videoRamAddr")
        xScroll = s.readInt("xScroll")
        tmpVideoRamAddr = s.readInt("tmpVideoRamAddr")
        writeToggle = s.readBoolean("writeToggle")
        highBitShift = s.readInt("highBitShift")
        lowBitShift = s.readInt("lowBitShift")
    }
}
