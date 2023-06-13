package br.tiagohm.nestalgia.core

abstract class FlashSST39SF040Mapper(console: Console) : Mapper(console) {

    protected lateinit var flash: FlashSST39SF040
        private set

    protected abstract val orgPrgRom: IntArray

    override fun initialize() {
        flash = FlashSST39SF040(prgRom)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("flash", flash)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshotable("flash", flash)
    }

    override fun readRegister(addr: Int): Int {
        val data = flash.read(addr)
        return if (data >= 0) data else internalRead(addr)
    }

    override fun saveBattery() {
        val ipsData = IpsPatcher.create(orgPrgRom, prgRom)

        if (ipsData.size > 8) {
            console.batteryManager.saveBattery(".ips", ipsData)
        }
    }

    protected fun applySaveData() {
        val ipsData = console.batteryManager.loadBattery(".ips")
        applyPatch(ipsData)
    }

    protected fun applyPatch(ipsData: IntArray) {
        if (ipsData.isNotEmpty()) {
            IpsPatcher.patch(ipsData, orgPrgRom).copyInto(prgRom)
        }
    }
}
