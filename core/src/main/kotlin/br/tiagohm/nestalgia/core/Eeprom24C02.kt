package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.Eeprom24C0X.Mode.*

@Suppress("NOTHING_TO_INLINE")
class Eeprom24C02(private val console: Console) : Eeprom24C0X(256) {

    @Volatile private var chipAddress = 0

    init {
        console.initializeRam(romData)

        loadBattery()
    }

    private inline fun writeAddressBit(value: Int) {
        if (counter < 8) {
            val mask = (1 shl 7 - counter).inv()
            address = (address and mask) or (value shl 7 - counter)
            counter++
        }
    }

    private inline fun writeDataBit(value: Int) {
        if (counter < 8) {
            val mask = (1 shl 7 - counter).inv()
            data = (data and mask) or (value shl 7 - counter)
            counter++
        }
    }

    private inline fun writeChipAddressBit(value: Int) {
        if (counter < 8) {
            val mask = (1 shl 7 - counter).inv()
            chipAddress = (chipAddress and mask) or (value shl 7 - counter)
            counter++
        }
    }

    private inline fun readBit() {
        if (counter < 8) {
            output = if (data and (1 shl 7 - counter) != 0) 1 else 0
            counter++
        }
    }

    override fun write(scl: Int, sda: Int) {
        if (prevScl != 0 && scl != 0 && sda < prevSda) {
            // START is identified by a high to low transition of the SDA
            // line while the clock SCL is *stable* in the high state.
            mode = CHIP_ADDRESS
            counter = 0
            output = 1
        } else if (prevScl != 0 && scl != 0 && sda > prevSda) {
            // STOP is identified by a low to high transition of the SDA
            // line while the clock SCL is *stable* in the high state.
            mode = IDLE
            output = 1
        } else if (scl > prevScl) {
            //Clock rise.
            when (mode) {
                CHIP_ADDRESS -> writeChipAddressBit(sda)
                ADDRESS -> writeAddressBit(sda)
                READ -> readBit()
                WRITE -> writeDataBit(sda)
                SEND_ACK -> output = 0
                WAIT_ACK -> if (sda == 0) {
                    nextMode = READ
                    data = romData[address]
                }
                else -> Unit
            }
        } else if (scl < prevScl) {
            // Clock fall.
            when (mode) {
                CHIP_ADDRESS ->
                    // Upon a correct compare the X24C02 outputs an
                    // acknowledge on the SDA line.
                    if (counter == 8) {
                        if (chipAddress and 0xA0 == 0xA0) {
                            mode = SEND_ACK
                            counter = 0
                            output = 1

                            // The last bit of the slave address defines the operation to
                            // be performed. When set to one a read operation is
                            // selected, when set to zero a write operations is selected
                            if (chipAddress.bit0) {
                                // Current Address Read
                                // Upon receipt of the slave address with the R/W
                                // bit set to one, the X24C02 issues an acknowledge
                                // and transmits the eight bit word during the next eight clock cycles
                                nextMode = READ
                                data = romData[address]
                            } else {
                                nextMode = ADDRESS
                            }
                        } else {
                            // This chip wasn't selected, go back to idle mode.
                            mode = IDLE
                            counter = 0
                            output = 1
                        }
                    }
                ADDRESS -> if (counter == 8) {
                    // Finished receiving all 8 bits of the address, send an ack and
                    // then starting writing the value.
                    counter = 0
                    mode = SEND_ACK
                    nextMode = WRITE
                    output = 1
                }
                READ -> if (counter == 8) {
                    // Finished sending all 8 bits, wait for an ack.
                    mode = WAIT_ACK
                    address = (address + 1) and 0xFF
                }
                WRITE -> if (counter == 8) {
                    // Finished receiving all 8 bits, send an ack.
                    counter = 0
                    mode = SEND_ACK
                    nextMode = WRITE
                    romData[address] = data
                    address = (address + 1) and 0xFF
                }
                SEND_ACK, WAIT_ACK -> {
                    mode = nextMode
                    counter = 0
                    output = 1
                }
                else -> Unit
            }
        }

        prevScl = scl
        prevSda = sda
    }

    override fun saveBattery() {
        console.batteryManager.saveBattery(".eeprom256", romData)
    }

    override fun loadBattery() {
        console.batteryManager.loadBattery(".eeprom256").copyInto(romData)
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("chipAddress", chipAddress)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        chipAddress = s.readInt("chipAddress")
    }
}
