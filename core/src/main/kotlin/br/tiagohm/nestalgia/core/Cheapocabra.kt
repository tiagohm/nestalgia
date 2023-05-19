package br.tiagohm.nestalgia.core


// https://wiki.nesdev.com/w/index.php/INES_Mapper_111
// Único jogo "Ninja Ryukenden (China)" e não roda (tela verde).

class Cheapocabra : FlashSST39SF040Mapper() {

    override val prgPageSize = 0x8000U

    override val chrPageSize = 0x2000U

    override val workRamSize = 0U

    override val saveRamSize = 0U

    override val registerStartAddress: UShort = 0x5000U

    override val registerEndAddress: UShort = 0x5FFFU

    override val chrRamSize = 0x4000U

    override val allowRegisterRead = true

    private var prgReg: UByte = 0U

    override fun init() {
        super.init()

        addRegisterRange(0x7000U, 0x7FFFU, MemoryOperation.WRITE)

        addRegisterRange(0x8000U, 0xFFFFU, MemoryOperation.ANY)
        removeRegisterRange(0x5000U, 0x5FFFU, MemoryOperation.READ)

        writeRegister(0x5000U, getPowerOnByte())

        orgPrgRom = prgRom.copyOf()
        applySaveData()
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if (addr.toInt() < 0x8000) {
            prgReg = value and 0x0FU

            selectPrgPage(0U, prgReg.toUShort())
            selectChrPage(0U, (value.toUShort() shr 4) and 0x01U)

            // TODO: Mesen itera até 8, mas causa erro de "Invalid PPU address range".
            repeat(4) {
                setNametable(it, (if (value.bit5) 8 else 0) + it)
            }
        } else {
            flash.write((prgReg.toInt() shl 15) or (addr.toInt() and 0x7FFF), value)
        }
    }
}
