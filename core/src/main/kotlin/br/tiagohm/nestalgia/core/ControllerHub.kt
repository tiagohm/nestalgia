package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*
import org.slf4j.LoggerFactory

abstract class ControllerHub(
    val portCount: Int,
    console: Console, type: ControllerType, port: Int,
    vararg controllers: ControllerSettings,
) : ControlDevice(console, type, port) {

    private val ports = arrayOfNulls<ControlDevice>(PORT_COUNT)

    init {
        for (i in controllers.indices) {
            val controller = controllers[i]

            val device = when (controller.type) {
                FAMICOM_CONTROLLER,
                FAMICOM_CONTROLLER_P2,
                NES_CONTROLLER -> StandardController(console, controller.type, 0, controller.keyMapping)
                else -> continue
            }

            LOG.info("{} connected. type={}, port={}", device::class.simpleName, device.type, device.port)

            ports[i] = device
        }
    }

    fun controlDevice(port: Int): ControlDevice? {
        return ports[port]
    }

    override fun hasControllerType(type: ControllerType): Boolean {
        return super.hasControllerType(type) || ports.any { it != null && it.hasControllerType(type) }
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

    companion object {

        private val LOG = LoggerFactory.getLogger(ControllerHub::class.java)
    }
}
