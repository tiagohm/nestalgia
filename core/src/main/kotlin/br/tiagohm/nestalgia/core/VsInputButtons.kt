package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.VsButton.*

class VsInputButtons(console: Console) : ControlDevice(console, NONE, MAPPER_INPUT_PORT) {

    private val needInsertCoin = IntArray(2)
    @Volatile private var needServiceButton = false

    private fun processInsertCoin(port: Int) {
        if (needInsertCoin[port] > 0) {
            needInsertCoin[port]--

            setBit(INSERT_COIN_1.bit + port)
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        var value = 0

        if (addr == 0x4016) {
            if (isPressed(INSERT_COIN_1)) value = value or 0x20
            if (isPressed(INSERT_COIN_2)) value = value or 0x40
            if (isPressed(SERVICE)) value = value or 0x04
        }

        return value
    }

    override fun onAfterSetState() {
        processInsertCoin(0)
        processInsertCoin(1)

        if (needServiceButton) {
            setBit(SERVICE)
            needServiceButton = false
        }
    }

    fun insertCoin(port: Int) {
        if (port in 0..1) {
            console.pause()
            needInsertCoin[port] = INSERT_COIN_FRAME_COUNT
            console.resume()
        }
    }

    fun pressServiceButton() {
        console.pause()
        needServiceButton = true
        console.resume()
    }

    companion object {

        private const val INSERT_COIN_FRAME_COUNT = 4
    }
}
