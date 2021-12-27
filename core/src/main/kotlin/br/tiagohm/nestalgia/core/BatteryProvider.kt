package br.tiagohm.nestalgia.core

interface BatteryProvider {
    fun loadBattery(name: String): UByteArray

    fun saveBattery(name: String, data: UByteArray)
}