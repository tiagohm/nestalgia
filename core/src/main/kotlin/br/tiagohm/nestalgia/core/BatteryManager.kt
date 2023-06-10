package br.tiagohm.nestalgia.core

import kotlin.math.max

class BatteryManager(private val console: Console) {

    var saveEnabled = false
    var provider: BatteryProvider? = null

    fun initialize() {
        saveEnabled = true
    }

    fun loadBattery(name: String, length: Int = -1): IntArray {
        val batteryName = "${console.mapper!!.info.hash.md5}$name"

        try {
            if (provider != null) {
                val data = provider!!.loadBattery(batteryName)

                return if (data.isEmpty()) {
                    IntArray(max(0, length))
                } else if (data.size >= length) {
                    data
                } else {
                    IntArray(length).also(data::copyInto)
                }
            }
        } catch (e: Throwable) {
            console.notificationManager.sendNotification(
                NotificationType.ERROR,
                "Cannot load battery from $batteryName"
            )
        }

        return IntArray(0)
    }

    fun saveBattery(name: String, data: IntArray) {
        if (saveEnabled && provider != null) {
            val batteryName = "${console.mapper!!.info.hash.md5}$name"

            try {
                provider!!.saveBattery(batteryName, data)
            } catch (e: Throwable) {
                console.notificationManager.sendNotification(
                    NotificationType.ERROR,
                    "Cannot save battery at $batteryName"
                )
            }
        }
    }
}
