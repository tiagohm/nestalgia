package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Family_Computer_Disk_System

@Suppress("NOTHING_TO_INLINE")
class Fds : Mapper() {

    private lateinit var audio: FdsAudio

    private var disableAutoInsertDisk = false

    // Write registers
    private var irqReloadValue: UShort = 0U
    private var irqCounter: UShort = 0U
    private var irqEnabled = false
    private var irqRepeatEnabled = false

    private var diskRegEnabled = true
    private var soundRegEnabled = true

    private var writeDataReg: UByte = 0U

    private var motorOn = false
    private var resetTransfer = false
    private var readMode = false
    private var crcControl = false
    private var diskReady = false
    private var diskIrqEnabled = false

    private var autoDiskEjectCounter = -1
    private var autoDiskSwitchCounter = -1
    private var restartAutoInsertCounter = -1
    private var previousFrame = 0
    private var lastDiskCheckFrame = 0
    private var successiveChecks = 0
    private var previousDiskNumber = NO_DISK_INSERTED

    private var extConWriteReg: UByte = 0U

    private var badCrc = false
    private var endOfHead = false
    private var readWriteEnabled = false

    private var readDataReg: UByte = 0U

    private var diskWriteProtected = false

    private var diskNumber = NO_DISK_INSERTED
    private var diskPosition = 0
    private var delay = 0L
    private var crcAccumulator: UShort = 0U
    private var previousCrcControlFlag = false
    private var gapEnded = true
    private var scanningDisk = false
    private var transferComplete = false

    private val diskSides = ArrayList<UByteArray>()
    private val diskHeaders = ArrayList<UByteArray>()
    private var rawData = UByteArray(0)

    private val orgDiskSides = ArrayList<UByteArray>()
    private val orgDiskHeaders = ArrayList<UByteArray>()

    private var isNeedSave = false
    private var gameStarted = false

    override val prgPageSize = 0x2000U

    override val chrPageSize = 0x2000U

    override val workRamPageSize = 0x8000U

    override val workRamSize = 0x8000U

    override val registerStartAddress: UShort = 0x4020U

    override val registerEndAddress: UShort = 0x4092U

    override val allowRegisterRead = true

    val isAutoInsertDiskEnabled: Boolean
        get() = !disableAutoInsertDisk && console.settings.checkFlag(EmulationFlag.FDS_AUTO_INSERT_DISK)

    val sideCount: Int
        get() = diskSides.size

    val isDiskInserted: Boolean
        get() = diskNumber != NO_DISK_INSERTED

    val currentDisk: Int
        get() = diskNumber

    override fun init() {
        // FDS BIOS
        setCpuMemoryMapping(0xE000U, 0xFFFFU, 0, PrgMemoryType.ROM, MemoryAccessType.READ)
        // WRAM
        setCpuMemoryMapping(0x6000U, 0xDFFFU, 0, PrgMemoryType.WRAM, MemoryAccessType.READ_WRITE)
        // 8K of CHR RAM
        selectChrPage(0U, 0U)

        audio = FdsAudio(console)

        rawData = data.bytes.toUByteArray()

        FdsLoader.loadDiskData(rawData, orgDiskSides, orgDiskHeaders)
        loadDiskData(console.batteryManager.loadBattery(".ips"))
    }

    private fun loadDiskData(ipsData: UByteArray) {
        diskSides.clear()
        diskHeaders.clear()

        val patchedData = ArrayList<UByte>(rawData.size)

        if (ipsData.isNotEmpty() && IpsPatcher.patch(ipsData, rawData, patchedData)) {
            FdsLoader.loadDiskData(patchedData.toUByteArray(), diskSides, diskHeaders)
        } else {
            FdsLoader.loadDiskData(rawData, diskSides, diskHeaders)
        }
    }

    private fun createIpsPatch(): UByteArray {
        val needHeader = rawData.startsWith("FDS\u001A")
        val newData = FdsLoader.rebuildFdsFile(diskSides, needHeader)
        return IpsPatcher.createPatch(rawData, newData)
    }

    override fun saveBattery() {
        if (isNeedSave) {
            val ipsData = createIpsPatch()
            console.batteryManager.saveBattery(".ips", ipsData)
            isNeedSave = false
        }
    }

    override fun reset(softReset: Boolean) {
        autoDiskEjectCounter = -1
        autoDiskSwitchCounter = -1
        disableAutoInsertDisk = false
        gameStarted = false
    }

    private inline fun getFdsDiskSideSize(side: Int): Int {
        return diskSides[side].size
    }

    private inline fun readFdsDisk() = diskSides[diskNumber][diskPosition]

    private inline fun writeFdsDisk(value: UByte) {
        if (diskNumber < diskSides.size &&
            diskPosition >= 2 &&
            diskPosition < diskSides[diskNumber].size
        ) {
            val currentValue = diskSides[diskNumber][diskPosition - 2]

            if (currentValue != value) {
                diskSides[diskNumber][diskPosition - 2] = value
                isNeedSave = true
            }
        }
    }

    private inline fun clockIrq() {
        if (irqEnabled) {
            if (irqCounter.toInt() == 0) {
                console.cpu.setIRQSource(IRQSource.EXTERNAL)

                irqCounter = irqReloadValue

                if (!irqRepeatEnabled) {
                    irqEnabled = false
                }
            } else {
                irqCounter--
            }
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        if (addr.toInt() == 0xE18C &&
            !gameStarted &&
            (console.memoryManager.peek(0x100U) and 0xC0U).toInt() != 0
        ) {
            // $E18B is the NMI entry point (using $E18C due to dummy reads)
            // When NMI occurs while $100 & $C0 != 0, it typically means that the game is starting.
            gameStarted = true
        } else if (addr.toInt() == 0xE445 && isAutoInsertDiskEnabled) {
            // Game is trying to check if a specific disk/side is inserted
            // Find the matching disk and insert it automatically
            val bufferAddr = console.memoryManager.peekWord(0U).toInt()

            val buffer = UByteArray(10)

            for (i in 0..9) {
                // Prevent infinite recursion
                if (bufferAddr + i != 0xE445) {
                    buffer[i] = console.memoryManager.peek((bufferAddr + i).toUShort())
                } else {
                    buffer[i] = 0U
                }
            }

            var matchCount = 0
            var matchIndex = -1

            for (j in diskHeaders.indices) {
                var match = true

                for (i in 0..9) {
                    if (buffer[i].toInt() != 0xFF && buffer[i] != diskHeaders[j][i + 15]) {
                        match = false
                        break
                    }
                }

                if (match) {
                    matchCount++
                    matchIndex = if (matchCount > 1) -1 else j
                }
            }

            if (matchCount > 1) {
                // More than 1 disk matches, can happen in unlicensed games - disable auto insert logic
                disableAutoInsertDisk = true
            }

            if (matchIndex >= 0) {
                // Found a single match, insert it
                diskNumber = matchIndex

                if (diskNumber != previousDiskNumber) {
                    val side = if (diskNumber and 0x01 == 0) "A" else "B"
                    System.err.println("[FDS] Disk automatically inserted: Disk ${diskNumber / 2 + 1} Side $side")
                    previousDiskNumber = diskNumber
                }

                if (matchIndex > 0) {
                    // Make sure we disable fast forward
                    gameStarted = true
                }
            }

            // Prevent disk from being switched again until the disk is actually read
            autoDiskSwitchCounter = -1
            restartAutoInsertCounter = -1
        }

        return super.read(addr, type)
    }

    private inline fun processAutoDiskInsert() {
        if (isAutoInsertDiskEnabled) {
            val currentFrame = console.ppu.frameCount

            if (previousFrame != currentFrame) {
                previousFrame = currentFrame

                if (autoDiskEjectCounter > 0) {
                    // After reading a disk, wait until this counter reaches 0 before
                    // automatically ejecting the disk the next time $4032 is read
                    autoDiskEjectCounter--
                } else if (autoDiskSwitchCounter > 0) {
                    // After ejecting the disk, wait a bit before we insert a new one
                    autoDiskSwitchCounter--

                    if (autoDiskSwitchCounter == 0) {
                        // Insert a disk (real disk/side will be selected when game executes $E445
                        System.err.println("[FDS] Auto-inserted dummy disk")
                        insertDisk(0)
                        // Restart process after 200 frames if the game hasn't read the disk yet
                        restartAutoInsertCounter = 200
                    }
                } else if (restartAutoInsertCounter > 0) {
                    // After ejecting the disk, wait a bit before we insert a new one
                    restartAutoInsertCounter--

                    if (restartAutoInsertCounter == 0) {
                        // Wait a bit before ejecting the disk (eject in ~34 frames)
                        console.notificationManager.sendNotification(
                            NotificationType.ERROR,
                            "Game failed to load disk, try again"
                        )
                        previousDiskNumber = NO_DISK_INSERTED
                        autoDiskEjectCounter = 34
                        autoDiskSwitchCounter = -1
                    }
                }
            }
        }
    }

    override fun processCpuClock() {
        processAutoDiskInsert()

        clockIrq()
        audio.clock()

        if (!isDiskInserted || !motorOn) {
            // Disk has been ejected
            endOfHead = true
            scanningDisk = false
            return
        }

        if (resetTransfer && !scanningDisk) {
            return
        }

        if (endOfHead) {
            delay = 50000
            endOfHead = false
            diskPosition = 0
            gapEnded = false
            return
        }

        if (delay > 0) {
            delay--
        } else {
            scanningDisk = true
            autoDiskEjectCounter = -1
            autoDiskSwitchCounter = -1

            var diskData: UByte = 0U
            var needIrq = diskIrqEnabled

            if (readMode) {
                diskData = readFdsDisk()

                if (!previousCrcControlFlag) {
                    updateCrc(diskData)
                }

                if (!diskReady) {
                    gapEnded = false
                    crcAccumulator = 0U
                } else if (diskData > 0U && !gapEnded) {
                    gapEnded = true
                    needIrq = false
                }

                if (gapEnded) {
                    transferComplete = true
                    readDataReg = diskData

                    if (needIrq) {
                        console.cpu.setIRQSource(IRQSource.FDS_DISK)
                    }
                }
            } else {
                if (!crcControl) {
                    transferComplete = true
                    diskData = writeDataReg

                    if (needIrq) {
                        console.cpu.setIRQSource(IRQSource.FDS_DISK)
                    }
                }

                if (!diskReady) {
                    diskData = 0x00U
                }

                if (!crcControl) {
                    updateCrc(diskData)
                } else {
                    if (!previousCrcControlFlag) {
                        // Finish CRC calculation
                        updateCrc(0x00U)
                        updateCrc(0x00U)
                    }

                    diskData = crcAccumulator.loByte
                    crcAccumulator = crcAccumulator shr 8
                }

                writeFdsDisk(diskData)
                gapEnded = false
            }

            previousCrcControlFlag = crcControl

            diskPosition++

            if (diskPosition >= getFdsDiskSideSize(diskNumber)) {
                motorOn = false
                // Wait a bit before ejecting the disk (eject in ~77 frames)
                autoDiskEjectCounter = 77
            } else {
                delay = 150
            }
        }
    }

    private inline fun updateCrc(value: UByte) {
        var n = 0x01

        while (n <= 0x80) {
            val carry = crcAccumulator.bit0

            crcAccumulator = crcAccumulator shr 1

            if (carry) {
                crcAccumulator = crcAccumulator xor 0x8408U
            }

            if ((value.toInt() and n) != 0) {
                crcAccumulator = crcAccumulator xor 0x8000U
            }

            n = n shl 1
        }
    }

    override fun writeRegister(addr: UShort, value: UByte) {
        if ((!diskRegEnabled && addr in 0x4024U..0x4026U) ||
            (!soundRegEnabled && addr >= 0x4040U)
        ) {
            return
        }

        when (addr.toInt()) {
            0x4020 -> irqReloadValue = (irqReloadValue and 0xFF00U) or value.toUShort()
            0x4021 -> irqReloadValue = (irqReloadValue and 0x00FFU) or (value.toInt() shl 8).toUShort()
            0x4022 -> {
                irqRepeatEnabled = value.bit0
                irqEnabled = value.bit1 and diskRegEnabled

                if (irqEnabled) {
                    irqCounter = irqReloadValue
                } else {
                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                }
            }
            0x4023 -> {
                diskRegEnabled = value.bit0
                soundRegEnabled = value.bit1

                if (!diskRegEnabled) {
                    irqEnabled = false
                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                    console.cpu.clearIRQSource(IRQSource.FDS_DISK)
                }
            }
            0x4024 -> {
                writeDataReg = value
                transferComplete = false

                // Unsure about clearing irq here: FCEUX/Nintendulator don't do this, puNES does.
                console.cpu.clearIRQSource(IRQSource.FDS_DISK)
            }
            0x4025 -> {
                motorOn = value.bit0
                resetTransfer = value.bit1
                readMode = value.bit2
                mirroringType = if (value.bit3) MirroringType.HORIZONTAL else MirroringType.VERTICAL
                crcControl = value.bit4
                // Bit 5 is not used, always 1
                diskReady = value.bit6
                diskIrqEnabled = value.bit7

                // Writing to $4025 clears IRQ according to FCEUX, puNES & Nintendulator
                // Fixes issues in some unlicensed games (error $20 at power on)
                console.cpu.clearIRQSource(IRQSource.FDS_DISK)
            }
            0x4026 -> extConWriteReg = value
            else -> {
                if (addr >= 0x4040U) audio.write(addr, value)
            }
        }
    }

    override fun readRegister(addr: UShort): UByte {
        var value = console.memoryManager.getOpenBus()

        if (soundRegEnabled && addr >= 0x4040U) {
            return audio.read(addr)
        } else if (diskRegEnabled && addr <= 0x4033U) {
            when (addr.toInt()) {
                0x4030 -> {
                    // These 3 pins are open bus
                    value = value and 0x2CU

                    if (console.cpu.hasIRQSource(IRQSource.EXTERNAL)) value = value or 0x01U
                    if (transferComplete) value = value or 0x02U
                    if (badCrc) value = value or 0x10U
                    // if(endOfHead) value = value or 0x40U
                    // if(diskRegEnabled) value = value or 0x80U

                    transferComplete = false

                    console.cpu.clearIRQSource(IRQSource.EXTERNAL)
                    console.cpu.clearIRQSource(IRQSource.FDS_DISK)

                    return value
                }
                0x4031 -> {
                    transferComplete = false
                    console.cpu.clearIRQSource(IRQSource.FDS_DISK)
                    return readDataReg
                }
                0x4032 -> {
                    //These 5 pins are open bus
                    value = value and 0xF8U

                    if (!isDiskInserted) value = value or 0x01U // Disk not in drive
                    if (!isDiskInserted || !scanningDisk) value = value or 0x02U // Disk not ready
                    if (!isDiskInserted) value = value or 0x04U // Disk not writable

                    if (isAutoInsertDiskEnabled) {
                        if (console.ppu.frameCount - lastDiskCheckFrame < 100) {
                            successiveChecks++
                        } else {
                            successiveChecks = 0
                        }

                        lastDiskCheckFrame = console.ppu.frameCount

                        if (successiveChecks > 20 &&
                            autoDiskEjectCounter == 0 &&
                            autoDiskSwitchCounter == -1
                        ) {
                            // Game tried to check if a disk was inserted or not
                            // - this is usually done when the disk needs to be changed
                            // Eject the current disk and insert the new one in ~77 frames
                            lastDiskCheckFrame = 0
                            successiveChecks = 0
                            autoDiskSwitchCounter = 77
                            previousDiskNumber = diskNumber
                            diskNumber = NO_DISK_INSERTED

                            System.err.println("[FDS] Disk automatically ejected")
                        }
                    }

                    return value
                }
                0x4033 -> {
                    //Always return good battery
                    return extConWriteReg
                }
            }
        }

        return console.memoryManager.getOpenBus()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("irqReloadValue", irqReloadValue)
        s.write("irqCounter", irqCounter)
        s.write("irqEnabled", irqEnabled)
        s.write("irqRepeatEnabled", irqRepeatEnabled)
        s.write("diskRegEnabled", diskRegEnabled)
        s.write("soundRegEnabled", soundRegEnabled)
        s.write("writeDataReg", writeDataReg)
        s.write("motorOn", motorOn)
        s.write("resetTransfer", resetTransfer)
        s.write("readMode", readMode)
        s.write("crcControl", crcControl)
        s.write("diskReady", diskReady)
        s.write("diskIrqEnabled", diskIrqEnabled)
        s.write("extConWriteReg", extConWriteReg)
        s.write("badCrc", badCrc)
        s.write("endOfHead", endOfHead)
        s.write("readWriteEnabled", readWriteEnabled)
        s.write("readDataReg", readDataReg)
        s.write("diskWriteProtected", diskWriteProtected)
        s.write("diskNumber", diskNumber)
        s.write("diskPosition", diskPosition)
        s.write("delay", delay)
        s.write("previousCrcControlFlag", previousCrcControlFlag)
        s.write("gapEnded", gapEnded)
        s.write("scanningDisk", scanningDisk)
        s.write("transferComplete", transferComplete)
        s.write("audio", audio)
        s.write("autoDiskEjectCounter", autoDiskEjectCounter)
        s.write("autoDiskSwitchCounter", autoDiskSwitchCounter)
        s.write("restartAutoInsertCounter", restartAutoInsertCounter)
        s.write("previousFrame", previousFrame)
        s.write("lastDiskCheckFrame", lastDiskCheckFrame)
        s.write("successiveChecks", successiveChecks)
        s.write("previousDiskNumber", previousDiskNumber)
        s.write("crcAccumulator", crcAccumulator)

        for (i in diskSides.indices) {
            val ipsData = IpsPatcher.createPatch(orgDiskSides[i], diskSides[i])
            s.write("ipsData$i", ipsData)
        }
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqReloadValue = s.readUShort("irqReloadValue") ?: 0U
        irqCounter = s.readUShort("irqCounter") ?: 0U
        irqEnabled = s.readBoolean("irqEnabled") ?: false
        irqRepeatEnabled = s.readBoolean("irqRepeatEnabled") ?: false
        diskRegEnabled = s.readBoolean("diskRegEnabled") ?: true
        soundRegEnabled = s.readBoolean("soundRegEnabled") ?: true
        writeDataReg = s.readUByte("writeDataReg") ?: 0U
        motorOn = s.readBoolean("motorOn") ?: false
        resetTransfer = s.readBoolean("resetTransfer") ?: false
        readMode = s.readBoolean("readMode") ?: false
        crcControl = s.readBoolean("crcControl") ?: false
        diskReady = s.readBoolean("diskReady") ?: false
        diskIrqEnabled = s.readBoolean("diskIrqEnabled") ?: false
        extConWriteReg = s.readUByte("extConWriteReg") ?: 0U
        badCrc = s.readBoolean("badCrc") ?: false
        endOfHead = s.readBoolean("endOfHead") ?: false
        readWriteEnabled = s.readBoolean("readWriteEnabled") ?: false
        readDataReg = s.readUByte("readDataReg") ?: 0U
        diskWriteProtected = s.readBoolean("diskWriteProtected") ?: false
        diskNumber = s.readInt("diskNumber") ?: 0
        diskPosition = s.readInt("diskPosition") ?: 0
        delay = s.readLong("delay") ?: 0L
        previousCrcControlFlag = s.readBoolean("previousCrcControlFlag") ?: false
        gapEnded = s.readBoolean("gapEnded") ?: true
        scanningDisk = s.readBoolean("scanningDisk") ?: false
        transferComplete = s.readBoolean("transferComplete") ?: false
        s.readSnapshot("audio")?.let { audio.restoreState(it) }
        autoDiskEjectCounter = s.readInt("autoDiskEjectCounter") ?: -1
        autoDiskSwitchCounter = s.readInt("autoDiskSwitchCounter") ?: -1
        restartAutoInsertCounter = s.readInt("restartAutoInsertCounter") ?: -1
        previousFrame = s.readInt("previousFrame") ?: 0
        lastDiskCheckFrame = s.readInt("lastDiskCheckFrame") ?: 0
        successiveChecks = s.readInt("successiveChecks") ?: 0
        previousDiskNumber = s.readInt("previousDiskNumber") ?: NO_DISK_INSERTED
        crcAccumulator = s.readUShort("crcAccumulator") ?: 0U

        for (i in diskSides.indices) {
            val ipsData = s.readUByteArray("ipsData$i") ?: continue
            val output = ArrayList<UByte>(FdsLoader.FDS_DISK_SIDE_CAPACITY)

            if (IpsPatcher.patch(ipsData, orgDiskSides[i], output)) {
                diskSides[i] = output.toUByteArray()
            }
        }

        // Make sure we disable fast forwarding when loading a state
        // Otherwise it's possible to get stuck in fast forward mode
        gameStarted = true
    }

    override val availableFeatures = listOf(ConsoleFeature.FDS)

    fun ejectDisk() {
        diskNumber = NO_DISK_INSERTED
    }

    fun insertDisk(diskNumber: Int) {
        if (!isDiskInserted) this.diskNumber = diskNumber % sideCount
    }

    companion object {
        const val NO_DISK_INSERTED = 0xFF
    }
}