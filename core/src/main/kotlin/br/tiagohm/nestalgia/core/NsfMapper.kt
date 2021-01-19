package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class NsfMapper : Mapper() {

    override val controlDevice: ControlDevice
        get() = TODO("Not yet implemented")

    override val dipSwitchCount: Int
        get() = TODO("Not yet implemented")

    override fun dispose() {
        console.settings.disableOverclocking = false
        console.settings.clearFlag(EmulationFlag.NSF_PLAYER_ENABLED)
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        TODO("Not yet implemented")
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        TODO("Not yet implemented")
    }
}