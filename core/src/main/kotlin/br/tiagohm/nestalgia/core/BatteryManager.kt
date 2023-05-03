package br.tiagohm.nestalgia.core

import kotlin.math.min

class BatteryManager(val console: Console) {

    var isSaveEnabled = false

    var provider: BatteryProvider? = null

    fun initialize() {
        isSaveEnabled = true
    }

    fun loadBattery(name: String, length: Int = -1): UByteArray {
        val batteryName = "${console.mapper!!.name}$name"

        try {
            if (provider != null) {
                return if (length >= 0) {
                    val data = UByteArray(length)
                    val batteryData = provider!!.loadBattery(batteryName)

                    for (i in 0 until min(data.size, batteryData.size)) {
                        data[i] = batteryData[i]
                    }

                    data
                } else {
                    provider!!.loadBattery(batteryName)
                }
            }
        } catch (e: Exception) {
            console.notificationManager.sendNotification(
                NotificationType.ERROR,
                "Cannot load battery from $batteryName"
            )
        }

        return UByteArray(0)
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
