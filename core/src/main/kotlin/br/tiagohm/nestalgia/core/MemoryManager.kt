package br.tiagohm.nestalgia.core

import java.util.*

@ExperimentalUnsignedTypes
class MemoryManager constructor(private val console: Console) :
    Memory,
    Peekable,
    Snapshotable {

    private val openBusHandler = OpenBusHandler()
    private val ramReadHandlers = Array<MemoryHandler>(RAM_SIZE) { openBusHandler }
    private val ramWriteHandlers = Array<MemoryHandler>(RAM_SIZE) { openBusHandler }
    private val internalRam = UByteArray(INTERNAL_RAM_SIZE)
    private val internalRamHandler = InternalRamHandler(internalRam, 0x7FFU)

    init {
        registerIODevice(internalRamHandler)
    }

    var mapper: Mapper? = null

    fun reset(softReset: Boolean) {
        if (!softReset) {
            console.initializeRam(internalRam)
        }

        mapper!!.reset(softReset)
    }

    fun registerIODevice(handler: MemoryHandler) {
        val ranges = MemoryRanges().also { handler.getMemoryRanges(it) }
        initializeMemoryHandlers(ramReadHandlers, handler, ranges.ramReadAddresses, ranges.allowOverride)
        initializeMemoryHandlers(ramWriteHandlers, handler, ranges.ramWriteAddresses, ranges.allowOverride)
    }

    fun registerWriteHandler(handler: MemoryHandler, start: Int, end: Int) {
        (start until end).forEach { ramWriteHandlers[it] = handler }
    }

    fun unregisterIODevice(handler: MemoryHandler) {
        val ranges = MemoryRanges().also { handler.getMemoryRanges(it) }
        ranges.ramReadAddresses.forEach { ramReadHandlers[it.toInt()] = openBusHandler }
        ranges.ramWriteAddresses.forEach { ramWriteHandlers[it.toInt()] = openBusHandler }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        val handler = ramReadHandlers[addr.toInt()]
        val value = console.cheatManager.applyCode(addr, handler.read(addr))
        console.debugger.processRamOperation(type, addr, value)
        openBusHandler.openBus = value
        return value
    }

    override fun peek(addr: UShort): UByte {
        val value = if (addr.toInt() <= 0x1FFF) {
            ramReadHandlers[addr.toInt()].read(addr)
        } else {
            ramReadHandlers[addr.toInt()].peek(addr)
        }

        return console.cheatManager.applyCode(addr, value)
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        console.debugger.processRamOperation(type, addr, value)
        val handler = ramWriteHandlers[addr.toInt()]
        handler.write(addr, value)
    }

    private fun initializeMemoryHandlers(
        handlers: Array<MemoryHandler>,
        handler: MemoryHandler,
        addresses: ArrayList<UShort>,
        allowOverride: Boolean
    ) {
        for (addr in addresses) {
            if (!allowOverride && handlers[addr.toInt()] != openBusHandler && handlers[addr.toInt()] != handler) {
                throw IllegalStateException("Not supported")
            } else {
                handlers[addr.toInt()] = handler
            }
        }
    }

    fun getOpenBus(mask: UByte = 0xFFU): UByte {
        return openBusHandler.openBus and mask
    }

    override fun saveState(s: Snapshot) {
        s.write("internalRam", internalRam)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        s.readUByteArray("internalRam")?.copyInto(internalRam)
    }

    companion object {
        const val RAM_SIZE = 0x10000
        const val VRAM_SIZE = 0x4000
        const val INTERNAL_RAM_SIZE = 0x800
    }
}
