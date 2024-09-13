package br.tiagohm.nestalgia.core

sealed interface Battery {

    fun saveBattery()

    fun loadBattery()
}
