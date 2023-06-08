package br.tiagohm.nestalgia.core

import kotlin.math.min

class BatteryManager(private val console: Console) {

    var saveEnabled = false
    var provider: BatteryProvider? = null

    fun initialize() {
        saveEnabled = true
    }

    fun loadBattery(name: String, length: Int = -1): IntArray {
        val batteryName = "${console.mapper!!.name}$name"

        try {
            if (provider != null) {
                return if (length >= 0) {
                    val batteryData = provider!!.loadBattery(batteryName)
                    batteryData.copyOfRange(0, min(length, batteryData.size))
                } else {
                    provider!!.loadBattery(batteryName)
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
            val batteryName = "${console.mapper!!.name}$name"

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
