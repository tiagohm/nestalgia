package br.tiagohm.nestalgia.core

open class ControllerHub(val portCount: Int, console: Console, type: ControllerType, port: Int) : ControlDevice(console, type, port) {

    private val ports = arrayOfNulls<ControlDevice>(PORT_COUNT)

    fun controlDevice(port: Int): ControlDevice? {
        return if (port in 0 until portCount) ports[port] else null
    }

    operator fun contains(type: ControllerType): Boolean {
        return if (this.type == type) true
        else ports.any { it != null && it.type == type }
    }

    override fun setStateFromInput() {
        ports.forEach { it?.setStateFromInput() }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)
        repeat(portCount) { ports[it]?.write(addr, value, type) }
    }
}
