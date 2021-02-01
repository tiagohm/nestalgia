package br.tiagohm.nestalgia.core

import java.io.IOException
import kotlin.math.min
import kotlin.random.Random

// https://wiki.nesdev.com/w/index.php/Mapper

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
abstract class Mapper :
    Resetable,
    Battery,
    Peekable,
    MemoryHandler,
    Disposable,
    Snapshotable {

    open var region: Region = Region.AUTO

    open val controlDevice: ControlDevice? = null

    open val prgPageSize = 0U

    open val chrPageSize = 0U

    open val chrRamSize = 0U

    open val dipSwitchCount = 0

    open val isForceSaveRamSize = false

    open val isForceWorkRamSize = false

    open val isForceChrBattery = false

    open val allowRegisterRead = false

    open val saveRamSize
        get() = if (hasBattery) 0x2000U else 0U

    open val saveRamPageSize = 0x2000U

    open val workRamSize
        get() = if (hasBattery) 0U else 0x2000U

    open val workRamPageSize = 0x2000U

    open val registerStartAddress: UShort = 0x8000U

    open val registerEndAddress: UShort = 0xFFFFU

    open val hasBusConflicts = false

    open val chrRamPageSize = 0x2000U

    private val isReadRegisterAddr = BooleanArray(0x10000)
    private val isWriteRegisterAddr = BooleanArray(0x10000)

    protected var privateSaveRamSize = 0U
        private set
    protected var privateWorkRamSize = 0U
        private set
    protected var privatePrgSize = 0U
        private set
    protected var privateChrRomSize = 0U
        private set
    protected var privateChrRamSize = 0U
        private set
    protected var onlyChrRam = false
        private set

    protected var vramOpenBusValue: Short = -1

    // Make sure the page size is no bigger than the size of the ROM itself
    // Otherwise we will end up reading from unallocated memory

    private inline val internalPrgPageSize: UInt
        get() = min(prgPageSize, privatePrgSize)

    private inline val internalSaveRamPageSize: UInt
        get() = min(saveRamPageSize, privateSaveRamSize)

    private inline val internalWorkRamPageSize: UInt
        get() = min(workRamPageSize, privateWorkRamSize)

    private inline val internalChrPageSize: UInt
        get() = min(chrPageSize, privateChrRomSize)

    private inline val internalChrRamPageSize: UInt
        get() = min(chrRamPageSize, privateChrRamSize)

    protected val prgPageCount: UInt
        get() {
            val pageSize = internalPrgPageSize
            return if (pageSize > 0U) privatePrgSize / pageSize else 0U
        }

    protected val chrPageCount: UInt
        get() {
            val pageSize = internalChrPageSize
            return if (pageSize > 0U) privateChrRomSize / pageSize else 0U
        }

    open val hasBattery: Boolean
        get() = info.hasBattery

    lateinit var console: Console
        internal set

    lateinit var data: RomData
        private set

    inline val info: RomInfo
        get() = data.info

    val name: String
        get() = if (::data.isInitialized) info.name else ""

    val hasChrRam: Boolean
        get() = privateChrRamSize > 0U

    val hasChrRom: Boolean
        get() = !onlyChrRam

    protected var prgRom = UByteArray(0)
        private set
    protected var chrRom = UByteArray(0)
        private set
    protected var chrRam = UByteArray(0)
        private set
    protected var saveRam = UByteArray(0)
        private set
    protected var workRam = UByteArray(0)
        private set
    protected var nametableRam = UByteArray(0)
        private set

    private var hasChrBattery = false
    private var privateHasBusConflicts = false

    private val prgMemoryAccess = Array(0x100) { MemoryAccessType.NO_ACCESS }
    private val prgPages = Array(0x100) { Pointer.NULL }
    private val prgMemoryOffset = IntArray(0x100)
    private val prgMemoryType = Array(0x100) { PrgMemoryType.ROM }
    private val chrMemoryAccess = Array(0x100) { MemoryAccessType.NO_ACCESS }
    private val chrPages = Array(0x100) { Pointer.NULL }
    private val chrMemoryOffset = IntArray(0x100)
    private val chrMemoryType = Array(0x100) { ChrMemoryType.DEFAULT }

    open fun init() {
    }

    override fun dispose() {
    }

    open fun init(data: RomData) {
    }

    override fun reset(softReset: Boolean) {
    }

    protected open fun writeRegister(addr: UShort, value: UByte) {
    }

    protected open fun readRegister(addr: UShort): UByte = 0U

    override fun saveBattery() {
        if (hasBattery && privateSaveRamSize > 0U) {
            console.batteryManager.saveBattery(".sav", saveRam)
        }
        if (hasChrBattery && privateChrRamSize > 0U) {
            console.batteryManager.saveBattery(".sav.chr", chrRam)
        }
    }

    override fun loadBattery() {
        if (hasBattery && privateSaveRamSize > 0U) {
            saveRam = console.batteryManager.loadBattery(".sav", privateSaveRamSize.toInt())
        }
        if (hasChrBattery && privateChrRamSize > 0U) {
            chrRam = console.batteryManager.loadBattery(".sav.chr", privateChrRamSize.toInt())
        }
    }

    open fun processCpuClock() {
    }

    fun copyPrgChrRom(mapper: Mapper) {
        if (privatePrgSize == mapper.privatePrgSize &&
            privateChrRomSize == mapper.privateChrRomSize
        ) {
            mapper.prgRom.copyInto(prgRom, 0, 0, privatePrgSize.toInt())

            if (!onlyChrRam) {
                mapper.chrRom.copyInto(chrRom, 0, 0, privateChrRomSize.toInt())
            }
        }
    }

    private var privateMirroringType: MirroringType? = null
    var mirroringType: MirroringType?
        get() = privateMirroringType
        set(value) {
            if (value != null) {
                privateMirroringType = value

                when (value) {
                    MirroringType.VERTICAL -> setNametables(0, 1, 0, 1)
                    MirroringType.HORIZONTAL -> setNametables(0, 0, 1, 1)
                    MirroringType.FOUR_SCREENS -> setNametables(0, 1, 2, 3)
                    MirroringType.SCREEN_A_ONLY -> setNametables(0, 0, 0, 0)
                    MirroringType.SCREEN_B_ONLY -> setNametables(1, 1, 1, 1)
                }
            }
        }

    private inline fun setNametables(a: Int, b: Int, c: Int, d: Int) {
        setNametable(0U, a)
        setNametable(1U, b)
        setNametable(2U, c)
        setNametable(3U, d)
    }

    fun setNametable(index: UByte, nametableIndex: Int) {
        if (nametableIndex in 0 until NAMETABLE_COUNT) {
            setPpuMemoryMapping(
                (0x2000U + index * 0x400U).toUShort(),
                (0x2000U + (index + 1U) * 0x400U - 1U).toUShort(),
                nametableIndex.toUShort(),
                ChrMemoryType.NAMETABLE_RAM
            )
            // Mirror $2000-$2FFF to $3000-$3FFF, while keeping a distinction between the addresses
            // Previously, $3000-$3FFF was being "redirected" to $2000-$2FFF to avoid MMC3 IRQ issues (which is incorrect)
            // More info here: https://forums.nesdev.com/viewtopic.php?p=132145#p132145
            setPpuMemoryMapping(
                (0x3000U + index * 0x400U).toUShort(),
                (0x3000U + (index + 1U) * 0x400U - 1U).toUShort(),
                nametableIndex.toUShort(),
                ChrMemoryType.NAMETABLE_RAM
            )
        } else {
            throw IllegalArgumentException("Invalid nametable index")
        }
    }

    fun getNametable(nametableIndex: Int): Pointer {
        if (nametableIndex in 0 until NAMETABLE_COUNT) {
            return Pointer(nametableRam, nametableIndex * NAMETABLE_SIZE)
        } else {
            throw IllegalArgumentException("Invalid nametable index")
        }
    }

    fun initialize(data: RomData) {
        this.data = data

        privateSaveRamSize = if (data.saveRamSize == -1 || isForceSaveRamSize) {
            saveRamSize
        } else {
            data.saveRamSize.toUInt()
        }

        privateWorkRamSize = if (data.workRamSize == -1 || isForceWorkRamSize) {
            workRamSize
        } else {
            data.workRamSize.toUInt()
        }

        isReadRegisterAddr.fill(false)
        isWriteRegisterAddr.fill(false)

        addRegisterRange(registerStartAddress, registerEndAddress, MemoryOperation.ANY)

        privatePrgSize = data.prgRom.size.toUInt()
        privateChrRomSize = data.chrRom.size.toUInt()

        prgRom = UByteArray(privatePrgSize.toInt())
        chrRom = UByteArray(privateChrRomSize.toInt())

        data.prgRom.copyInto(prgRom)
        data.chrRom.copyInto(chrRom)

        hasChrBattery = data.saveChrRamSize > 0 || isForceChrBattery

        privateHasBusConflicts = when (data.info.busConflict) {
            BusConflictType.DEFAULT -> hasBusConflicts
            BusConflictType.YES -> true
            BusConflictType.NO -> false
        }

        if (privateHasBusConflicts) {
            System.err.println("Bus conflicts enabled")
        }

        saveRam = UByteArray(privateSaveRamSize.toInt())
        workRam = UByteArray(privateWorkRamSize.toInt())

        console.initializeRam(saveRam)
        console.initializeRam(workRam)

        nametableRam = UByteArray(NAMETABLE_SIZE * NAMETABLE_COUNT)
        console.initializeRam(nametableRam)

        for (i in 0..0xff) {
            // Allow us to map a different page every 256 bytes
            prgPages[i] = Pointer.NULL
            prgMemoryOffset[i] = -1
            prgMemoryType[i] = PrgMemoryType.ROM
            prgMemoryAccess[i] = MemoryAccessType.NO_ACCESS

            chrPages[i] = Pointer.NULL
            chrMemoryOffset[i] = -1
            chrMemoryType[i] = ChrMemoryType.DEFAULT
            chrMemoryAccess[i] = MemoryAccessType.NO_ACCESS
        }

        when {
            privateChrRomSize == 0U -> {
                // Assume there is CHR RAM if no CHR ROM exists
                onlyChrRam = true
                initializeChrRam(data.chrRamSize)
                // Map CHR RAM to 0x0000-0x1FFF by default when no CHR ROM exists
                setPpuMemoryMapping(0x0000U, 0x1FFFU, 0U, ChrMemoryType.RAM)
                privateChrRomSize = privateChrRamSize
            }
            data.chrRamSize >= 0 -> {
                initializeChrRam(data.chrRamSize)
            }
            chrRamSize > 0U -> {
                initializeChrRam()
            }
        }

        if (info.hasTreiner) {
            if (privateWorkRamSize >= 0x2000U) {
                System.arraycopy(data.treinerData, 0, workRam, 0x1000, 512)
                data.treinerData.copyInto(workRam, 0x1000, 0, 512)
            } else if (privateSaveRamSize >= 0x2000U) {
                data.treinerData.copyInto(saveRam, 0x1000, 0, 512)
            }
        }

        setupDefaultWorkRam()

        mirroringType = data.info.mirroring

        val info = data.info.copy(
            hasChrRam = hasChrRam,
            busConflict = if (privateHasBusConflicts) BusConflictType.YES else BusConflictType.NO
        )

        this.data = data.copy(info = info)

        init()
        init(data)

        loadBattery()
    }

    override fun getMemoryRanges(ranges: MemoryRanges) {
        if (info.system == GameSystem.VS_SYSTEM) {
            ranges.addHandler(MemoryOperation.READ, 0x6000U, 0xFFFFU)
            ranges.addHandler(MemoryOperation.WRITE, 0x6000U, 0xFFFFU)
        } else {
            ranges.addHandler(MemoryOperation.READ, 0x4018U, 0xFFFFU)
            ranges.addHandler(MemoryOperation.WRITE, 0x4018U, 0xFFFFU)
        }
    }

    override fun read(addr: UShort, type: MemoryOperationType): UByte {
        return if (allowRegisterRead && isReadRegisterAddr[addr.toInt()]) {
            readRegister(addr)
        } else {
            val hi = addr.hiByte.toInt()

            if (prgMemoryAccess[hi].isRead) {
                prgPages[hi][addr.loByte.toInt()]
            } else {
                console.memoryManager.getOpenBus()
            }
        }
    }

    override fun peek(addr: UShort): UByte {
        val hi = addr.hiByte.toInt()
        return if (prgMemoryAccess[hi].isRead) prgPages[hi][addr.loByte.toInt()] else (addr shr 8).toUByte()
    }

    inline fun readVRAM(addr: UShort) = mapperReadVRAM(addr)

    @PublishedApi
    internal open fun mapperReadVRAM(addr: UShort) = internalReadVRAM(addr)

    @PublishedApi
    internal fun internalReadVRAM(addr: UShort): UByte {
        val hi = addr.hiByte.toInt()

        if (chrMemoryAccess[hi].isRead) {
            return chrPages[hi][addr.loByte.toInt()]
        }

        // Open bus - "When CHR is disabled, the pattern tables are open bus.
        // Theoretically, this should return the LSB of the address read, but real-world behavior varies."
        return if (vramOpenBusValue >= 0) vramOpenBusValue.toUByte() else addr.toUByte()
    }

    override fun write(addr: UShort, value: UByte, type: MemoryOperationType) {
        if (isWriteRegisterAddr[addr.toInt()]) {
            if (privateHasBusConflicts) {
                val hi = addr.hiByte.toInt()
                val lo = addr.loByte.toInt()
                writeRegister(addr, value and prgPages[hi][lo])
            } else {
                writeRegister(addr, value)
            }
        } else {
            writePrgRam(addr, value)
        }
    }

    open fun writePrgRam(addr: UShort, value: UByte) {
        val hi = addr.hiByte.toInt()

        if (prgMemoryAccess[hi].isWrite) {
            val page = prgPages[hi]
            page[addr.loByte.toInt()] = value
        }
    }

    fun writeVRAM(addr: UShort, value: UByte) {
        val hi = addr.hiByte.toInt()

        if (chrMemoryAccess[hi].isWrite) {
            val page = chrPages[hi]
            page[addr.loByte.toInt()] = value
        }
    }

    protected fun validateAddressRange(start: UShort, end: UShort): Boolean {
        // Start/End address must be multiples of 256/0x100
        return start.loByte.isZero && end.loByte.isFilled
    }

    private fun setupDefaultWorkRam() {
        // Setup a default work/save ram in 0x6000-0x7FFF space
        if (hasBattery && privateSaveRamSize > 0U) {
            setCpuMemoryMapping(0x6000U, 0x7FFFU, 0, PrgMemoryType.SRAM)
        } else if (privateWorkRamSize > 0U) {
            setCpuMemoryMapping(0x6000U, 0x7FFFU, 0, PrgMemoryType.WRAM)
        }
    }

    protected fun setCpuMemoryMapping(
        start: UShort,
        end: UShort,
        pageNumber: Short,
        type: PrgMemoryType,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0xFF00U || end <= start) {
            System.err.println("Invalid address range")
            return
        }

        val pageCount: UInt
        val pageSize: UInt
        var defaultAccessType = MemoryAccessType.READ

        when (type) {
            PrgMemoryType.ROM -> {
                pageCount = prgPageCount
                pageSize = internalPrgPageSize
            }
            PrgMemoryType.SRAM -> {
                pageSize = internalSaveRamPageSize

                if (pageSize == 0U) {
                    // System.err.println("DEBUG: Tried to map undefined save ram.")
                    return
                }

                pageCount = privateSaveRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
            else -> {
                pageSize = internalWorkRamPageSize

                if (pageSize == 0U) {
                    // System.err.println("DEBUG: Tried to map undefined work ram.")
                    return
                }

                pageCount = privateWorkRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
        }

        if (pageCount == 0U) {
            // System.err.println("DEBUG: Tried to map undefined save/work ram.")
            return
        }

        fun wrapPageNumber(page: Short): Short {
            return if (page < 0) {
                // Can't use modulo for negative number because pageCount is sometimes not a power of 2.  (Fixes some Mapper 191 games)
                (pageCount.toInt() + page).toShort()
            } else {
                (page % pageCount.toInt()).toShort()
            }
        }

        var page = wrapPageNumber(pageNumber)

        if (end - start >= pageSize) {
            // System.err.println("DEBUG: Tried to map undefined prg - page size too small for selected range.")

            var addr = start.toUInt()

            // If range is bigger than a single page, keep going until we reach the last page
            while (addr <= end - pageSize + 1U) {
                setCpuMemoryMapping(
                    addr.toUShort(),
                    (addr + pageSize - 1U).toUShort(),
                    type,
                    page * pageSize.toInt(),
                    if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType
                )
                addr += pageSize
                page = wrapPageNumber((page + 1).toShort())
            }
        } else {
            setCpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize.toInt(),
                if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType
            )
        }
    }

    protected fun setCpuMemoryMapping(
        start: UShort,
        end: UShort,
        type: PrgMemoryType,
        sourceOffset: Int,
        accessType: MemoryAccessType,
    ) {
        val sourceMemory = when (type) {
            PrgMemoryType.ROM -> Pointer(prgRom)
            PrgMemoryType.SRAM -> Pointer(saveRam)
            else -> Pointer(workRam)
        }

        val firstSlot = start.toInt() shr 8
        val slotCount = (end.toInt() - start.toInt() + 1) shr 8

        for (i in 0 until slotCount) {
            prgMemoryOffset[firstSlot + i] = sourceOffset + i * 0x100
            prgMemoryType[firstSlot + i] = type
            prgMemoryAccess[firstSlot + i] = accessType
        }

        setCpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun setCpuMemoryMapping(
        start: UShort,
        end: UShort,
        pointer: Pointer,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start.toInt() shr 8
        val b = end.toInt() shr 8
        var source = pointer

        for (i in a..b) {
            prgPages[i] = source
            prgMemoryAccess[i] =
                if (accessType != MemoryAccessType.UNSPECIFIED) accessType else MemoryAccessType.READ

            if (source != Pointer.NULL) {
                source = Pointer(source, 0x100)
            }
        }
    }

    protected fun removeCpuMemoryMapping(start: UShort, end: UShort) {
        val firstSlot = start.toInt() shr 8
        val slotCount = (end.toInt() - start.toInt() + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        for (i in 0 until slotCount) {
            prgMemoryOffset[firstSlot + i] = -1
            prgMemoryType[firstSlot + i] = PrgMemoryType.ROM
            prgMemoryAccess[firstSlot + i] = MemoryAccessType.NO_ACCESS
        }

        setCpuMemoryMapping(start, end, Pointer.NULL, MemoryAccessType.NO_ACCESS)
    }

    protected fun setPpuMemoryMapping(
        start: UShort,
        end: UShort,
        pageNumber: UShort,
        type: ChrMemoryType,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0x3F00U || end > 0x3FFFU || end <= start) {
            System.err.println("Invalid address range")
            return
        }

        val pageCount: UInt
        val pageSize: UInt
        var defaultAccessType = MemoryAccessType.READ

        when (type) {
            ChrMemoryType.DEFAULT -> {
                pageSize = internalChrPageSize

                if (pageSize == 0U) {
                    // System.err.println("DEBUG: Tried to map undefined chr rom/ram.")
                    return
                }

                pageCount = chrPageCount

                if (onlyChrRam) {
                    defaultAccessType = MemoryAccessType.READ_WRITE
                }
            }
            ChrMemoryType.ROM -> {
                pageSize = internalChrPageSize

                if (pageSize == 0U) {
                    // System.err.println("DEBUG: Tried to map undefined chr rom.")
                    return
                }

                pageCount = chrPageCount
            }
            ChrMemoryType.RAM -> {
                pageSize = internalChrRamPageSize

                if (pageSize == 0U) {
                    // System.err.println("DEBUG: Tried to map undefined chr ram.")
                    return
                }

                pageCount = privateChrRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
            else -> {
                pageSize = NAMETABLE_SIZE.toUInt()
                pageCount = NAMETABLE_COUNT.toUInt()
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
        }

        if (pageCount == 0U) {
            // System.err.println("DEBUG: Tried to map undefined chr ram/ram.")
            return
        }

        var page = (pageNumber % pageCount).toInt()

        if (end - start >= pageSize) {
            var addr = start.toUInt()

            while (addr <= end - pageSize + 1U) {
                setPpuMemoryMapping(
                    addr.toUShort(),
                    (addr + pageSize - 1U).toUShort(),
                    type,
                    page * pageSize.toInt(),
                    accessType
                )
                addr += pageSize
                page = (page + 1) % pageCount.toInt()
            }
        } else {
            setPpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize.toInt(),
                if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType
            )
        }
    }

    protected fun setPpuMemoryMapping(
        start: UShort,
        end: UShort,
        type: ChrMemoryType,
        sourceOffset: Int,
        accessType: MemoryAccessType,
    ) {
        val sourceMemory: Pointer
        var sourceType = type

        when (sourceType) {
            ChrMemoryType.DEFAULT -> {
                sourceMemory =
                    if (onlyChrRam) Pointer(chrRam) else Pointer(chrRom)
                sourceType = if (onlyChrRam) ChrMemoryType.RAM else ChrMemoryType.ROM
            }
            ChrMemoryType.ROM -> sourceMemory = Pointer(chrRom)
            ChrMemoryType.RAM -> sourceMemory = Pointer(chrRam)
            else -> sourceMemory = Pointer(nametableRam)
        }

        val firstSlot = start.toInt() shr 8
        val slotCount = (end.toInt() - start.toInt() + 1) shr 8

        for (i in 0 until slotCount) {
            chrMemoryOffset[firstSlot + i] = sourceOffset + i * 256
            chrMemoryType[firstSlot + i] = sourceType
            chrMemoryAccess[firstSlot + i] = accessType
        }

        setPpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun setPpuMemoryMapping(
        start: UShort,
        end: UShort,
        pointer: Pointer,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start.toInt() shr 8
        val b = end.toInt() shr 8
        var sourceMemory = pointer

        for (i in a..b) {
            chrPages[i] = sourceMemory
            chrMemoryAccess[i] =
                if (accessType != MemoryAccessType.UNSPECIFIED) accessType else MemoryAccessType.READ_WRITE

            if (sourceMemory != Pointer.NULL) {
                sourceMemory = Pointer(sourceMemory, 0x100)
            }
        }
    }

    protected fun removePpuMemoryMapping(start: UShort, end: UShort) {
        val firstSlot = start.toInt() shr 8
        val slotCount = (end.toInt() - start.toInt() + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        for (i in 0 until slotCount) {
            chrMemoryOffset[firstSlot + i] = -1
            chrMemoryType[firstSlot + i] = ChrMemoryType.DEFAULT
            chrMemoryAccess[firstSlot + i] = MemoryAccessType.NO_ACCESS
        }

        setPpuMemoryMapping(start, end, Pointer.NULL, MemoryAccessType.NO_ACCESS)
    }

    private fun initializeChrRam(size: Int = -1) {
        val defaultRamSize = if (chrRamSize > 0U) chrRamSize else 0x2000U
        privateChrRamSize = if (size >= 0) size.toUInt() else defaultRamSize

        if (privateChrRamSize > 0U) {
            chrRam = UByteArray(privateChrRamSize.toInt())
            console.initializeRam(chrRam)
        }
    }

    protected fun addRegisterRange(start: UShort, end: UShort, operation: MemoryOperation = MemoryOperation.ANY) {
        for (i in start..end) {
            if (operation.isRead) {
                isReadRegisterAddr[i.toInt()] = true
            }
            if (operation.isWrite) {
                isWriteRegisterAddr[i.toInt()] = true
            }
        }
    }

    protected fun removeRegisterRange(start: UShort, end: UShort, operation: MemoryOperation = MemoryOperation.ANY) {
        for (i in start..end) {
            if (operation.isRead) {
                isReadRegisterAddr[i.toInt()] = false
            }
            if (operation.isWrite) {
                isWriteRegisterAddr[i.toInt()] = false
            }
        }
    }

    open fun selectPrgPage(slot: UShort, page: UShort, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        if (privatePrgSize < 0x8000U && prgPageSize > privatePrgSize) {
            // System.err.println("DEBUG: Total PRG size is smaller than available memory range")
            // Total PRG size is smaller than available memory range, map the entire PRG to all slots
            // i.e same logic as NROM (mapper 0) when PRG is 16kb
            // Needed by "Pyramid" (mapper 79)
            var i = 0U

            while (i < 0x8000U / privatePrgSize) {
                val start = 0x8000U + i * privatePrgSize
                val end = start + privatePrgSize - 1U
                setCpuMemoryMapping(start.toUShort(), end.toUShort(), 0, memoryType)
                i++
            }
        } else {
            val start = 0x8000U + slot * internalPrgPageSize
            val end = start + internalPrgPageSize - 1U
            setCpuMemoryMapping(start.toUShort(), end.toUShort(), page.toShort(), memoryType)
        }
    }

    protected fun readRam(addr: UShort): UByte {
        val page = prgPages[addr.hiByte.toInt()]
        return if (page != Pointer.NULL) page[addr.loByte.toInt()] else 0U
    }

    fun selectPrgPage4x(slot: UShort, page: UShort, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        selectPrgPage2x((slot * 2U).toUShort(), page, memoryType)
        selectPrgPage2x((slot * 2U + 1U).toUShort(), (page + 2U).toUShort(), memoryType)
    }

    fun selectPrgPage2x(slot: UShort, page: UShort, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        selectPrgPage((slot * 2U).toUShort(), page, memoryType)
        selectPrgPage((slot * 2U + 1U).toUShort(), (page + 1U).toUShort(), memoryType)
    }

    open fun selectChrPage(slot: UShort, page: UShort, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        val pageSize = when (memoryType) {
            ChrMemoryType.NAMETABLE_RAM -> NAMETABLE_SIZE.toUInt()
            ChrMemoryType.RAM -> internalChrRamPageSize
            else -> internalChrPageSize
        }

        val start = slot * pageSize
        val end = start + pageSize - 1U

        setPpuMemoryMapping(start.toUShort(), end.toUShort(), page, memoryType)
    }

    fun selectChrPage8x(slot: UShort, page: UShort, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage4x(slot, page, memoryType)
        selectChrPage4x((slot * 2U + 1U).toUShort(), (page + 4U).toUShort(), memoryType)
    }

    fun selectChrPage4x(slot: UShort, page: UShort, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage2x((slot * 2U).toUShort(), page, memoryType)
        selectChrPage2x((slot * 2U + 1U).toUShort(), (page + 2U).toUShort(), memoryType)
    }

    fun selectChrPage2x(slot: UShort, page: UShort, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage((slot * 2U).toUShort(), page, memoryType)
        selectChrPage((slot * 2U + 1U).toUShort(), (page + 1U).toUShort(), memoryType)
    }

    fun getPowerOnByte(default: UByte = 0U): UByte {
        return if (console.settings.checkFlag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)) {
            Random.nextInt(256).toUByte()
        } else {
            default
        }
    }

    open fun notifyVRAMAddressChange(addr: UShort) {
        // This is called when the VRAM addr on the PPU memory bus changes
        // Used by MMC3/MMC5/etc
    }

    inline fun debugReadVRAM(addr: UShort, disableSideEffects: Boolean = true): UByte {
        val a = addr and 0x3FFFU
        if (!disableSideEffects) notifyVRAMAddressChange(a)
        return internalReadVRAM(a)
    }

    open fun applySamples(buffer: ShortArray, sampleCount: Int, volume: Double) {
    }

    inline val dipSwitches: Int
        get() {
            val mask = (1 shl dipSwitchCount) - 1
            return console.settings.dipSwitches and mask
        }

    override fun saveState(s: Snapshot) {
        privateMirroringType?.let { s.write("mirroringType", it) }
        s.write("chrRam", chrRam)
        s.write("workRam", workRam)
        s.write("saveRam", saveRam)
        s.write("nametableRam", nametableRam)
        s.write("prgMemoryOffset", prgMemoryOffset)
        s.write("chrMemoryOffset", chrMemoryOffset)
        s.write("prgMemoryType", prgMemoryType)
        s.write("chrMemoryType", chrMemoryType)
        s.write("prgMemoryAccess", prgMemoryAccess)
        s.write("chrMemoryAccess", chrMemoryAccess)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        privateMirroringType = s.readEnum("mirroringType")
        s.readUByteArray("chrRam")?.copyInto(chrRam)
        s.readUByteArray("workRam")?.copyInto(workRam)
        s.readUByteArray("saveRam")?.copyInto(saveRam)
        s.readUByteArray("nametableRam")?.copyInto(nametableRam)
        s.readIntArray("prgMemoryOffset")?.copyInto(prgMemoryOffset)
        s.readIntArray("chrMemoryOffset")?.copyInto(chrMemoryOffset)
        s.readEnumArray<PrgMemoryType>("prgMemoryType")?.copyInto(prgMemoryType)
        s.readEnumArray<ChrMemoryType>("chrMemoryType")?.copyInto(chrMemoryType)
        s.readEnumArray<MemoryAccessType>("prgMemoryAccess")?.copyInto(prgMemoryAccess)
        s.readEnumArray<MemoryAccessType>("chrMemoryAccess")?.copyInto(chrMemoryAccess)

        restorePrgChrState()
    }

    private fun restorePrgChrState() {
        for (i in 0..0xff) {
            val startAddr = i shl 8

            if (prgMemoryAccess[i] != MemoryAccessType.NO_ACCESS) {
                setCpuMemoryMapping(
                    startAddr.toUShort(),
                    (startAddr + 0xFF).toUShort(),
                    prgMemoryType[i],
                    prgMemoryOffset[i],
                    prgMemoryAccess[i]
                )
            } else {
                removeCpuMemoryMapping(startAddr.toUShort(), (startAddr + 0xFF).toUShort())
            }
        }

        for (i in 0..0x3f) {
            val startAddr = i shl 8

            if (chrMemoryAccess[i] != MemoryAccessType.NO_ACCESS) {
                setPpuMemoryMapping(
                    startAddr.toUShort(),
                    (startAddr + 0xFF).toUShort(),
                    chrMemoryType[i],
                    chrMemoryOffset[i],
                    chrMemoryAccess[i]
                )
            } else {
                removePpuMemoryMapping(startAddr.toUShort(), (startAddr + 0xFF).toUShort())
            }
        }
    }

    open val availableFeatures: List<ConsoleFeature> = emptyList()

    fun toAbsoluteAddress(addr: UShort): Int {
        val prgAddr = prgPages[addr.hiByte.toInt()].offset + addr.loByte.toInt()
        return if (prgAddr >= 0 && prgAddr < privatePrgSize.toInt()) prgAddr else -1
    }

    companion object {

        fun initialize(
            console: Console,
            rom: ByteArray,
            name: String,
            fdsBios: ByteArray = ByteArray(0),
        ): Pair<Mapper?, RomData?> {
            val data = RomLoader.load(rom, name, fdsBios)

            if ((data.info.isInDatabase || data.info.isNes20Header) && data.info.inputType != GameInputType.UNSPECIFIED) {
                if (console.settings.checkFlag(EmulationFlag.AUTO_CONFIGURE_INPUT)) {
                    console.settings.initializeInputDevices(data.info.inputType, data.info.system)
                }
            } else if (data.info.isInDatabase) {
                val system = data.info.system
                val isFamicom = (system == GameSystem.FAMICOM || system == GameSystem.FDS || system == GameSystem.DENDY)
                console.settings.consoleType = if (isFamicom) ConsoleType.FAMICOM else ConsoleType.NES
            }

            return Pair(fromId(data), data)
        }

        fun fromId(data: RomData): Mapper {
            return when (val id = data.info.mapperId) {
                0 -> NROM()
                1 -> MMC1()
                2 -> UNROM()
                3 -> CNROM(false)
                4 -> if (data.info.subMapperId == 3) McAcc() else MMC3()
                12 -> Mapper012()
                14 -> Mapper014()
                36 -> Txc22000()
                37 -> Mapper037()
                44 -> Mapper044()
                45 -> Mapper045()
                47 -> Mapper047()
                49 -> Mapper049()
                52 -> Mapper052()
                74 -> Mapper074()
                91 -> Mapper091()
                108 -> Bb()
                114 -> Mapper114()
                115 -> Mapper115()
                118 -> TxSRom()
                119 -> Mapper119()
                121 -> Mapper121()
                123 -> Mapper123()
                132 -> Txc22211a()
                134 -> Mapper134()
                155 -> Mapper155()
                172 -> Txc22211b()
                173 -> Txc22211c()
                177 -> Henggedianzi177()
                179 -> Henggedianzi179()
                185 -> CNROM(true)
                190 -> MagicKidGooGoo()
                191 -> Mapper191()
                192 -> Mapper192()
                194 -> Mapper194()
                195 -> Mapper195()
                196 -> Mapper196()
                197 -> Mapper197()
                198 -> Mapper198()
                199 -> Mapper199()
                200 -> Mapper200()
                FDS_MAPPER_ID -> Fds()
                else -> {
                    System.err.println("${data.info.name} has unsupported mapper $id")
                    throw IOException("Unsupported mapper $id")
                }
            }
        }

        const val NAMETABLE_COUNT = 0x10
        const val NAMETABLE_SIZE = 0x400

        const val FDS_MAPPER_ID = 65535
    }
}
