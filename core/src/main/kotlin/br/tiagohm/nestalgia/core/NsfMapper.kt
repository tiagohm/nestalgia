package br.tiagohm.nestalgia.core

class NsfMapper(console: Console) : Mapper(console) {

    override val dipSwitchCount: Int
        get() = TODO("Not yet implemented")

    override fun initialize() {}

    override fun close() {
        console.settings.disableOverclocking = false
        console.settings.flag(EmulationFlag.NSF_PLAYER_ENABLED, false)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        TODO("Not yet implemented")
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        TODO("Not yet implemented")
    }
}
