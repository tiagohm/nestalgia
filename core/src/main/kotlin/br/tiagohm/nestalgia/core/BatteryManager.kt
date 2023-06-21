package br.tiagohm.nestalgia.core

class BatteryManager(private val console: Console) : Initializable {

    private var saveEnabled = false
    private var provider: BatteryProvider? = null

    override fun initialize() {
        saveEnabled = true
    }

    fun registerProvider(provider: BatteryProvider) {
        this.provider = provider
    }

    fun unregisterProvider() {
        provider = null
    }

    fun enable() {
        saveEnabled = true
    }

    fun disable() {
        saveEnabled = false
    }

    fun loadBattery(name: String, length: Int = -1): IntArray {
        val batteryName = "${console.mapper!!.info.hash.md5}$name"
        return provider?.loadBattery(batteryName) ?: IntArray(0)
    }

    fun saveBattery(name: String, data: IntArray) {
        if (saveEnabled) {
            val batteryName = "${console.mapper!!.info.hash.md5}$name"
            provider?.saveBattery(batteryName, data)
        }
    }
}
