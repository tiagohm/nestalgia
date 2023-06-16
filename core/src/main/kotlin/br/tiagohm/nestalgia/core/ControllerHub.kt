package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*

open class ControllerHub(
    val portCount: Int,
    console: Console, type: ControllerType, port: Int,
    vararg controllers: ControllerSettings,
) : ControlDevice(console, type, port) {

    private val ports = arrayOfNulls<ControlDevice>(PORT_COUNT)

    init {
        for (i in controllers.indices) {
            val controller = controllers[i]

            ports[i] = when (controller.type) {
                FAMICOM_CONTROLLER,
                FAMICOM_CONTROLLER_P2,
                NES_CONTROLLER -> StandardController(console, controller.type, 0, controller.keyMapping)
                else -> continue
            }
        }
    }

    fun controlDevice(port: Int): ControlDevice? {
        return ports[port]
    }

    operator fun contains(type: ControllerType): Boolean {
        return if (this.type == type) true
        else ports.any { it != null && it.type == type }
    }

    override fun setStateFromInput() {
        for (port in ports) {
            port?.clearState()
            port?.setStateFromInput()
        }
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        strobeOnWrite(value)

        repeat(portCount) {
            ports[it]?.write(addr, value, type)
        }
    }
}
