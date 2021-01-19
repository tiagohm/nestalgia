package br.tiagohm.nestalgia.core

import kotlin.math.min

@ExperimentalUnsignedTypes
class BatteryManager(val console: Console) {

    var isSaveEnabled = false

    var provider: BatteryProvider? = null

    fun initialize() {
        isSaveEnabled = true
    }

    fun loadBattery(name: String, length: Int): UByteArray {
        val data = UByteArray(length)
        val batteryName = "${console.mapper!!.name}$name"

        try {
            if (provider != null) {
                val batteryData = provider!!.loadBattery(batteryName)

                for (i in 0 until min(data.size, batteryData.size)) {
                    data[i] = batteryData[i]
                }
            }
        } catch (e: Exception) {
            console.notificationManager.sendNotification(
                NotificationType.ERROR,
                "Cannot load battery from $batteryName"
            )
        }

        return data
    }

    fun saveBattery(name: String, data: UByteArray) {
        if (isSaveEnabled && provider != null) {
            val batteryName = "${console.mapper!!.name}$name"

            try {
                provider!!.saveBattery(batteryName, data)
            } catch (e: Exception) {
                console.notificationManager.sendNotification(
                    NotificationType.ERROR,
                    "Cannot save battery at $batteryName"
                )
            }
        }
    }
}
