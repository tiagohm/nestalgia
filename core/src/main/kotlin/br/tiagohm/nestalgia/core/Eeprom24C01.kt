package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.Eeprom24C0X.Mode.*

@Suppress("NOTHING_TO_INLINE")
class Eeprom24C01(private val console: Console) : Eeprom24C0X(128) {

    init {
        console.initializeRam(romData)

        loadBattery()
    }

    private inline fun writeAddressBit(value: Int) {
        if (counter < 8) {
            val mask = (1 shl counter).inv()
            address = (address and mask) or (value shl counter)
            counter++
        }
    }

    private inline fun writeDataBit(value: Int) {
        if (counter < 8) {
            val mask = (1 shl counter).inv()
            data = (data and mask) or (value shl counter)
            counter++
        }
    }

    private inline fun readBit() {
        if (counter < 8) {
            output = if (data and (1 shl counter) != 0) 1 else 0
            counter++
        }
    }

    override fun write(scl: Int, sda: Int) {
        if (prevScl != 0 && scl != 0 && sda < prevSda) {
            // START is identified by a high to low transition of the SDA line
            // while the clock SCL is *stable* in the high state.
            mode = ADDRESS
            address = 0
            counter = 0
            output = 1
        } else if (prevScl != 0 && scl != 0 && sda > prevSda) {
            // STOP is identified by a low to high transition of the SDA line
            // while the clock SCL is *stable* in the high state.
            mode = IDLE
            output = 1
        } else if (scl > prevScl) {
            //Clock rise.
            when (mode) {
                ADDRESS ->
                    // To initiate a write operation, the master sends a start condition
                    // followed by a seven bit word address and a write bit.
                    if (counter < 7) {
                        writeAddressBit(sda)
                    } else if (counter == 7) {
                        // 8th bit to determine if we're in read or write mode.
                        counter = 8
                        if (sda != 0) {
                            // Read mode.
                            nextMode = READ
                            data = romData[address and 0x7F]
                        } else {
                            nextMode = WRITE
                        }
                    }
                SEND_ACK -> output = 0
                READ -> readBit()
                WRITE -> writeDataBit(sda)
                WAIT_ACK -> if (sda == 0) {
                    // We expected an ack, but received something else,
                    // return to idle mode.
                    nextMode = IDLE
                }
                else -> Unit
            }
        } else if (scl < prevScl) {
            // Clock fall.
            when (mode) {
                ADDRESS -> if (counter == 8) {
                    // After receiving the address, the X24C01 responds with
                    // an acknowledge, then waits for eight bits of data.
                    mode = SEND_ACK
                    output = 1
                }
                SEND_ACK -> {
                    // After sending an ack, move to the next mode of operation.
                    mode = nextMode
                    counter = 0
                    output = 1
                }
                READ -> if (counter == 8) {
                    // After sending all 8 bits, wait for an ack.
                    mode = WAIT_ACK
                    address = (address + 1) and 0x7F
                }
                WRITE -> if (counter == 8) {
                    // After receiving all 8 bits, send an ack and then wait.
                    mode = SEND_ACK
                    nextMode = IDLE
                    romData[address and 0x7F] = data
                    address = (address + 1) and 0x7F
                }
                else -> Unit
            }
        }

        prevScl = scl
        prevSda = sda
    }

    override fun saveBattery() {
        console.batteryManager.saveBattery(".eeprom128", romData)
    }

    override fun loadBattery() {
        console.batteryManager.loadBattery(".eeprom128").copyInto(romData)
    }
}
