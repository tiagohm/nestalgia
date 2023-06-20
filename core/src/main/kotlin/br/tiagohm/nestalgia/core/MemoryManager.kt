package br.tiagohm.nestalgia.core

class MemoryManager(private val console: Console) : Memory, Peekable, Resetable, Snapshotable {

    private val openBusHandler = OpenBusHandler()
    private val ramReadHandlers = Array<MemoryHandler>(RAM_SIZE) { openBusHandler }
    private val ramWriteHandlers = Array<MemoryHandler>(RAM_SIZE) { openBusHandler }
    private val internalRam = IntArray(INTERNAL_RAM_SIZE)
    private val internalRamHandler = InternalRamHandler(internalRam, 0x7FF)

    init {
        registerIODevice(internalRamHandler)
    }

    override fun reset(softReset: Boolean) {
        if (!softReset) {
            console.initializeRam(internalRam)
        }

        console.mapper?.reset(softReset)
    }

    fun registerIODevice(handler: MemoryHandler) {
        val ranges = MemoryRanges().also(handler::memoryRanges)
        initializeMemoryHandlers(ramReadHandlers, handler, ranges.ramReadAddresses, ranges.readSize, ranges.allowOverride)
        initializeMemoryHandlers(ramWriteHandlers, handler, ranges.ramWriteAddresses, ranges.writeSize, ranges.allowOverride)
    }

    fun registerWriteHandler(handler: MemoryHandler, start: Int, end: Int) {
        (start until end).forEach { ramWriteHandlers[it] = handler }
    }

    fun unregisterIODevice(handler: MemoryHandler) {
        val ranges = MemoryRanges().also(handler::memoryRanges)
        repeat(ranges.readSize) { ramReadHandlers[ranges.ramReadAddresses[it]] = openBusHandler }
        repeat(ranges.writeSize) { ramWriteHandlers[ranges.ramWriteAddresses[it]] = openBusHandler }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        val handler = ramReadHandlers[addr]
        val value = console.cheatManager.applyCode(addr, handler.read(addr))
        // console.debugger.processRamOperation(type, addr, value)
        openBusHandler.openBus = value
        return value
    }

    override fun peek(addr: Int): Int {
        val value = if (addr <= 0x1FFF) ramReadHandlers[addr].read(addr)
        else ramReadHandlers[addr].peek(addr)

        return console.cheatManager.applyCode(addr, value)
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        // console.debugger.processRamOperation(type, addr, value)
        val handler = ramWriteHandlers[addr]
        handler.write(addr, value and 0xFF)
        openBusHandler.openBus = value
    }

    private fun initializeMemoryHandlers(
        handlers: Array<MemoryHandler>,
        handler: MemoryHandler,
        addresses: IntArray,
        size: Int,
        allowOverride: Boolean,
    ) {
        repeat(size) {
            val addr = addresses[it]

            if (!allowOverride && handlers[addr] != openBusHandler && handlers[addr] != handler) {
                throw IllegalStateException("Not supported")
            } else {
                handlers[addr] = handler
            }
        }
    }

    fun openBus(mask: Int = 0xFF): Int {
        return openBusHandler.openBus and mask
    }

    override fun saveState(s: Snapshot) {
        s.write("internalRam", internalRam)
    }

    override fun restoreState(s: Snapshot) {
        s.readIntArray("internalRam", internalRam)
    }

    companion object {

        const val RAM_SIZE = 0x10000
        const val VRAM_SIZE = 0x4000
        const val INTERNAL_RAM_SIZE = 0x800
    }
}
