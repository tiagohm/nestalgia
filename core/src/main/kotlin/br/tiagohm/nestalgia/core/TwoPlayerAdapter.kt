package br.tiagohm.nestalgia.core

class TwoPlayerAdapter(console: Console, type: ControllerType) : ControllerHub(2, console, type, EXP_DEVICE_PORT) {

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        super.write(addr, value and 0x01, type)
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        strobeOnRead()

        var output = 0
        val i = addr - 0x4016

        val device = controlDevice(i)

        if (device != null) {
            output = device.read(0x4016) and 0x01 shl 1
        }

        return output
    }
}
