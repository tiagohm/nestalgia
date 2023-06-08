package br.tiagohm.nestalgia.core

interface BatteryProvider {

    fun loadBattery(name: String): IntArray

    fun saveBattery(name: String, data: IntArray)
}
