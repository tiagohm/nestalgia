package br.tiagohm.nestalgia.core

import java.io.Closeable

abstract class Eeprom24C0X(size: Int) : Battery, Closeable, Snapshotable {

    enum class Mode {
        IDLE,
        ADDRESS,
        READ,
        WRITE,
        SEND_ACK,
        WAIT_ACK,
        CHIP_ADDRESS,
    }

    @JvmField protected var mode = Mode.IDLE
    @JvmField protected var nextMode = Mode.IDLE
    @JvmField protected var address = 0
    @JvmField protected var data = 0
    @JvmField protected var counter = 0
    @JvmField protected var output = 0
    @JvmField protected var prevScl = 0
    @JvmField protected var prevSda = 0
    @JvmField protected val romData = IntArray(size)

    abstract fun write(scl: Int, sda: Int)

    fun read() = output

    fun writeScl(scl: Int) {
        write(scl, prevSda)
    }

    fun writeSda(sda: Int) {
        write(prevScl, sda)
    }

    override fun close() {
        saveBattery()
    }

    override fun saveState(s: Snapshot) {
        s.write("mode", mode)
        s.write("nextMode", nextMode)
        s.write("address", address)
        s.write("data", data)
        s.write("counter", counter)
        s.write("output", output)
        s.write("prevScl", prevScl)
        s.write("prevSda", prevSda)
        s.write("romData", romData)
    }

    override fun restoreState(s: Snapshot) {
        mode = s.readEnum("mode", Mode.IDLE)
        nextMode = s.readEnum("nextMode", Mode.IDLE)
        address = s.readInt("address")
        data = s.readInt("data")
        counter = s.readInt("counter")
        output = s.readInt("output")
        prevScl = s.readInt("prevScl")
        prevSda = s.readInt("prevSda")
        s.readIntArray("romData")?.copyInto(romData)
    }
}
