package br.tiagohm.nestalgia.core

abstract class FlashSST39SF040Mapper : Mapper() {

    protected lateinit var flash: FlashSST39SF040
    protected var orgPrgRom = UByteArray(0)

    override fun init() {
        super.init()

        flash = FlashSST39SF040(prgRom)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("flash", flash)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        s.readSnapshot("flash")?.also(flash::restoreState)
    }

    override fun readRegister(addr: UShort): UByte {
        val data = flash.read(addr.toInt())
        return if (data >= 0) data.toUByte() else internalRead(addr)
    }

    override fun saveBattery() {
        val ipsData = IpsPatcher.createPatch(orgPrgRom, prgRom)

        if (ipsData.size > 8) {
            console.batteryManager.saveBattery(".ips", ipsData)
        }
    }

    protected fun applySaveData() {
        val ipsData = console.batteryManager.loadBattery(".ips")
        applyPatch(ipsData)
    }

    protected fun applyPatch(ipsData: UByteArray) {
        if (ipsData.isNotEmpty()) {
            val patchedPrgRom = ArrayList<UByte>()

            if (IpsPatcher.patch(ipsData, orgPrgRom, patchedPrgRom)) {
                for (i in patchedPrgRom.indices) {
                    prgRom[i] = patchedPrgRom[i]
                }
            }
        }
    }
}
