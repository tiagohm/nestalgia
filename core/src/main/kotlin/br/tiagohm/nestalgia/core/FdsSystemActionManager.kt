package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
class FdsSystemActionManager(console: Console, mapper: Mapper) : SystemActionManager(console) {

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        TODO("Not yet implemented")
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        // TODO("Not yet implemented")
    }
}