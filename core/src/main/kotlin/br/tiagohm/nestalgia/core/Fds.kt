package br.tiagohm.nestalgia.core

// https://wiki.nesdev.com/w/index.php/Family_Computer_Disk_System

@Suppress("NOTHING_TO_INLINE")
class Fds(console: Console) : Mapper(console) {

    private lateinit var audio: FdsAudio

    private var disableAutoInsertDisk = false

    // Write registers
    private var irqReloadValue = 0
    private var irqCounter = 0
    private var irqEnabled = false
    private var irqRepeatEnabled = false

    private var diskRegEnabled = true
    private var soundRegEnabled = true

    private var writeDataReg = 0

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

    private var extConWriteReg = 0

    private var badCrc = false
    private var endOfHead = false
    private var readWriteEnabled = false

    private var readDataReg = 0

    private var diskWriteProtected = false

    private var diskNumber = NO_DISK_INSERTED
    private var diskPosition = 0
    private var delay = 0L
    private var crcAccumulator = 0
    private var previousCrcControlFlag = false
    private var gapEnded = true
    private var scanningDisk = false
    private var transferComplete = false

    private val diskSides = ArrayList<IntArray>()
    private val diskHeaders = ArrayList<IntArray>()
    private var rawData = IntArray(0)

    private val orgDiskSides = ArrayList<IntArray>()
    private val orgDiskHeaders = ArrayList<IntArray>()

    private var isNeedSave = false
    private var gameStarted = false

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val workRamPageSize = 0x8000

    override val workRamSize = 0x8000

    override val registerStartAddress = 0x4020

    override val registerEndAddress = 0x4092

    override val allowRegisterRead = true

    val autoInsertDiskEnabled
        get() = !disableAutoInsertDisk && console.settings.flag(EmulationFlag.FDS_AUTO_INSERT_DISK)

    val sideCount
        get() = diskSides.size

    val diskInserted
        get() = diskNumber != NO_DISK_INSERTED

    val currentDisk
        get() = diskNumber

    override fun initialize() {
        // FDS BIOS
        addCpuMemoryMapping(0xE000, 0xFFFF, 0, PrgMemoryType.ROM, MemoryAccessType.READ)
        // WRAM
        addCpuMemoryMapping(0x6000, 0xDFFF, 0, PrgMemoryType.WRAM, MemoryAccessType.READ_WRITE)
        // 8K of CHR RAM
        selectChrPage(0, 0)

        audio = FdsAudio(console)

        rawData = data.rawData

        FdsLoader.loadDiskData(rawData, orgDiskSides, orgDiskHeaders)
        loadDiskData(console.batteryManager.loadBattery(".ips"))
    }

    private fun loadDiskData(ipsData: IntArray) {
        diskSides.clear()
        diskHeaders.clear()

        if (ipsData.isNotEmpty()) {
            val patchedData = IpsPatcher.patch(ipsData, rawData)

            if (patchedData.isNotEmpty()) {
                FdsLoader.loadDiskData(patchedData, diskSides, diskHeaders)
            }
        } else {
            FdsLoader.loadDiskData(rawData, diskSides, diskHeaders)
        }
    }

    private fun createIpsPatch(): IntArray {
        val needHeader = rawData.startsWith("FDS\u001A")
        val newData = FdsLoader.rebuildFdsFile(diskSides, needHeader)
        return IpsPatcher.create(rawData, newData)
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

    private fun fdsDiskSideSize(side: Int): Int {
        return diskSides[side].size
    }

    private fun readFdsDisk() = diskSides[diskNumber][diskPosition]

    private fun writeFdsDisk(value: Int) {
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
            if (irqCounter == 0) {
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

    override fun read(addr: Int, type: MemoryOperationType): Int {
        if (addr == 0xE18C &&
            !gameStarted &&
            (console.memoryManager.peek(0x100) and 0xC0) != 0
        ) {
            // $E18B is the NMI entry point (using $E18C due to dummy reads)
            // When NMI occurs while $100 & $C0 != 0, it typically means that the game is starting.
            gameStarted = true
        } else if (addr == 0xE445 && autoInsertDiskEnabled) {
            // Game is trying to check if a specific disk/side is inserted
            // Find the matching disk and insert it automatically
            val bufferAddr = console.memoryManager.peekWord(0)

            val buffer = IntArray(10)

            for (i in 0..9) {
                // Prevent infinite recursion.
                if (bufferAddr + i != 0xE445) {
                    buffer[i] = console.memoryManager.peek(bufferAddr + i)
                } else {
                    buffer[i] = 0
                }
            }

            var matchCount = 0
            var matchIndex = -1

            for (j in diskHeaders.indices) {
                var match = true

                for (i in 0..9) {
                    if (buffer[i] != 0xFF && buffer[i] != diskHeaders[j][i + 15]) {
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
                // Found a single match, insert it.
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
        if (autoInsertDiskEnabled) {
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

        if (!diskInserted || !motorOn) {
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

            var diskData = 0
            var needIrq = diskIrqEnabled

            if (readMode) {
                diskData = readFdsDisk()

                if (!previousCrcControlFlag) {
                    updateCrc(diskData)
                }

                if (!diskReady) {
                    gapEnded = false
                    crcAccumulator = 0
                } else if (diskData > 0 && !gapEnded) {
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
                    diskData = 0x00
                }

                if (!crcControl) {
                    updateCrc(diskData)
                } else {
                    if (!previousCrcControlFlag) {
                        // Finish CRC calculation
                        updateCrc(0x00)
                        updateCrc(0x00)
                    }

                    diskData = crcAccumulator.loByte
                    crcAccumulator = crcAccumulator shr 8
                }

                writeFdsDisk(diskData)
                gapEnded = false
            }

            previousCrcControlFlag = crcControl

            diskPosition++

            if (diskPosition >= fdsDiskSideSize(diskNumber)) {
                motorOn = false
                // Wait a bit before ejecting the disk (eject in ~77 frames)
                autoDiskEjectCounter = 77
            } else {
                delay = 150
            }
        }
    }

    private inline fun updateCrc(value: Int) {
        var n = 0x01

        while (n <= 0x80) {
            val carry = crcAccumulator.bit0

            crcAccumulator = crcAccumulator shr 1

            if (carry) {
                crcAccumulator = crcAccumulator xor 0x8408
            }

            if ((value and n) != 0) {
                crcAccumulator = crcAccumulator xor 0x8000
            }

            n = n shl 1
        }
    }

    override fun writeRegister(addr: Int, value: Int) {
        if ((!diskRegEnabled && addr in 0x4024..0x4026) ||
            (!soundRegEnabled && addr >= 0x4040)
        ) {
            return
        }

        when (addr) {
            0x4020 -> irqReloadValue = (irqReloadValue and 0xFF00) or value
            0x4021 -> irqReloadValue = (irqReloadValue and 0x00FF) or (value shl 8)
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
                if (addr >= 0x4040) audio.write(addr, value)
            }
        }
    }

    override fun readRegister(addr: Int): Int {
        var value = console.memoryManager.openBus()

        if (soundRegEnabled && addr >= 0x4040) {
            return audio.read(addr)
        } else if (diskRegEnabled && addr <= 0x4033) {
            when (addr) {
                0x4030 -> {
                    // These 3 pins are open bus
                    value = value and 0x2C

                    if (console.cpu.hasIRQSource(IRQSource.EXTERNAL)) value = value or 0x01
                    if (transferComplete) value = value or 0x02
                    if (badCrc) value = value or 0x10
                    // if (endOfHead) value = value or 0x40
                    // if (diskRegEnabled) value = value or 0x80

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
                    value = value and 0xF8

                    if (!diskInserted) value = value or 0x01 // Disk not in drive
                    if (!diskInserted || !scanningDisk) value = value or 0x02 // Disk not ready
                    if (!diskInserted) value = value or 0x04 // Disk not writable

                    if (autoInsertDiskEnabled) {
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

        return console.memoryManager.openBus()
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
            val ipsData = IpsPatcher.create(orgDiskSides[i], diskSides[i])
            s.write("ipsData$i", ipsData)
        }
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        irqReloadValue = s.readInt("irqReloadValue")
        irqCounter = s.readInt("irqCounter")
        irqEnabled = s.readBoolean("irqEnabled")
        irqRepeatEnabled = s.readBoolean("irqRepeatEnabled")
        diskRegEnabled = s.readBoolean("diskRegEnabled", true)
        soundRegEnabled = s.readBoolean("soundRegEnabled", true)
        writeDataReg = s.readInt("writeDataReg")
        motorOn = s.readBoolean("motorOn")
        resetTransfer = s.readBoolean("resetTransfer")
        readMode = s.readBoolean("readMode")
        crcControl = s.readBoolean("crcControl")
        diskReady = s.readBoolean("diskReady")
        diskIrqEnabled = s.readBoolean("diskIrqEnabled")
        extConWriteReg = s.readInt("extConWriteReg")
        badCrc = s.readBoolean("badCrc")
        endOfHead = s.readBoolean("endOfHead")
        readWriteEnabled = s.readBoolean("readWriteEnabled")
        readDataReg = s.readInt("readDataReg")
        diskWriteProtected = s.readBoolean("diskWriteProtected")
        diskNumber = s.readInt("diskNumber")
        diskPosition = s.readInt("diskPosition")
        delay = s.readLong("delay")
        previousCrcControlFlag = s.readBoolean("previousCrcControlFlag")
        gapEnded = s.readBoolean("gapEnded", true)
        scanningDisk = s.readBoolean("scanningDisk")
        transferComplete = s.readBoolean("transferComplete")
        s.readSnapshotable("audio", audio)
        autoDiskEjectCounter = s.readInt("autoDiskEjectCounter", -1)
        autoDiskSwitchCounter = s.readInt("autoDiskSwitchCounter", -1)
        restartAutoInsertCounter = s.readInt("restartAutoInsertCounter", -1)
        previousFrame = s.readInt("previousFrame")
        lastDiskCheckFrame = s.readInt("lastDiskCheckFrame")
        successiveChecks = s.readInt("successiveChecks")
        previousDiskNumber = s.readInt("previousDiskNumber", NO_DISK_INSERTED)
        crcAccumulator = s.readInt("crcAccumulator")

        for (i in diskSides.indices) {
            val ipsData = s.readIntArray("ipsData$i") ?: continue

            val diskSide = IpsPatcher.patch(ipsData, orgDiskSides[i])

            if (diskSide.isNotEmpty()) {
                diskSides[i] = diskSide
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
        if (!diskInserted) this.diskNumber = diskNumber % sideCount
    }

    companion object {

        const val NO_DISK_INSERTED = 0xFF
    }
}
