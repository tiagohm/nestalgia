package br.tiagohm.nestalgia.core

fun interface ControlManagerListener {

    fun onControlDeviceChange(console: Console, device: ControlDevice)
}
