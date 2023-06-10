package br.tiagohm.nestalgia.core

import java.io.Closeable
import java.io.IOException
import kotlin.math.min
import kotlin.random.Random

// https://wiki.nesdev.com/w/index.php/Mapper

abstract class Mapper : Resetable, Battery, Peekable, MemoryHandler, Closeable, Snapshotable {

    open val controlDevice: ControlDevice? = null

    open val prgPageSize = 0

    open val chrPageSize = 0

    open val chrRamSize = 0

    open val dipSwitchCount = 0

    open val isForceSaveRamSize = false

    open val isForceWorkRamSize = false

    open val isForceChrBattery = false

    open val allowRegisterRead = false

    open val saveRamSize
        get() = if (hasBattery) 0x2000 else 0

    open val saveRamPageSize = 0x2000

    open val workRamSize
        get() = if (hasBattery) 0 else 0x2000

    open val workRamPageSize = 0x2000

    open val registerStartAddress = 0x8000

    open val registerEndAddress = 0xFFFF

    open val hasBusConflicts = false

    open val chrRamPageSize = 0x2000

    private val isReadRegisterAddr = BooleanArray(0x10000)
    private val isWriteRegisterAddr = BooleanArray(0x10000)

    protected var mSaveRamSize = 0
        private set

    protected var mWorkRamSize = 0
        private set

    protected var mPrgSize = 0
        private set

    protected var mChrRomSize = 0
        private set

    protected var mChrRamSize = 0
        private set

    protected var vramOpenBusValue = -1

    // Make sure the page size is no bigger than the size of the ROM itself
    // Otherwise we will end up reading from unallocated memory

    private val mPrgPageSize
        get() = min(prgPageSize, mPrgSize)

    private val mSaveRamPageSize
        get() = min(saveRamPageSize, mSaveRamSize)

    private val mWorkRamPageSize
        get() = min(workRamPageSize, mWorkRamSize)

    private val mChrPageSize
        get() = min(chrPageSize, mChrRomSize)

    private val mChrRamPageSize
        get() = min(chrRamPageSize, mChrRamSize)

    protected val prgPageCount
        get() = mPrgPageSize.let { if (it > 0) mPrgSize / it else 0 }

    protected val chrPageCount
        get() = mChrPageSize.let { if (it > 0) mChrRomSize / it else 0 }

    open val hasBattery
        get() = info.hasBattery

    lateinit var console: Console
        private set

    lateinit var data: RomData
        private set

    val info
        get() = data.info

    val name
        get() = if (::data.isInitialized) info.name else ""

    val hasChrRam
        get() = mChrRamSize > 0

    val hasChrRom
        get() = mChrRomSize > 0

    protected var prgRom = IntArray(0)
        private set

    protected var chrRom = IntArray(0)
        private set

    protected var chrRam = IntArray(0)
        private set

    protected var saveRam = IntArray(0)
        private set

    protected var workRam = IntArray(0)
        private set

    protected var nametableRam = IntArray(0)
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

    abstract fun initialize()

    override fun close() {}

    override fun reset(softReset: Boolean) {}

    protected open fun writeRegister(addr: Int, value: Int) {}

    protected open fun readRegister(addr: Int) = 0

    open fun updateRegion(region: Region) {}

    override fun saveBattery() {
        if (hasBattery && mSaveRamSize > 0) {
            console.batteryManager.saveBattery(".sav", saveRam)
        }
        if (hasChrBattery && mChrRamSize > 0) {
            console.batteryManager.saveBattery(".sav.chr", chrRam)
        }
    }

    override fun loadBattery() {
        if (hasBattery && mSaveRamSize > 0) {
            saveRam = console.batteryManager.loadBattery(".sav", mSaveRamSize)
        }
        if (hasChrBattery && mChrRamSize > 0) {
            chrRam = console.batteryManager.loadBattery(".sav.chr", mChrRamSize)
        }
    }

    open fun processCpuClock() {}

    fun copyPrgChrRom(mapper: Mapper) {
        if (mPrgSize == mapper.mPrgSize &&
            mChrRomSize == mapper.mChrRomSize
        ) {
            mapper.prgRom.copyInto(prgRom, 0, 0, mPrgSize)
            mapper.chrRom.copyInto(chrRom, 0, 0, mChrRomSize)
        }
    }

    private var mMirroringType = MirroringType.HORIZONTAL

    var mirroringType: MirroringType
        get() = mMirroringType
        set(value) {
            mMirroringType = value

            when (value) {
                MirroringType.VERTICAL -> nametables(0, 1, 0, 1)
                MirroringType.HORIZONTAL -> nametables(0, 0, 1, 1)
                MirroringType.FOUR_SCREENS -> nametables(0, 1, 2, 3)
                MirroringType.SCREEN_A_ONLY -> nametables(0, 0, 0, 0)
                MirroringType.SCREEN_B_ONLY -> nametables(1, 1, 1, 1)
            }
        }

    fun nametables(a: Int, b: Int, c: Int, d: Int) {
        nametable(0, a)
        nametable(1, b)
        nametable(2, c)
        nametable(3, d)
    }

    fun nametable(index: Int, nametableIndex: Int) {
        if (nametableIndex in 0 until NAMETABLE_COUNT) {
            addPpuMemoryMapping(
                0x2000 + index * 0x400,
                0x2000 + (index + 1) * 0x400 - 1,
                nametableIndex,
                ChrMemoryType.NAMETABLE_RAM,
            )
            // Mirror $2000-$2FFF to $3000-$3FFF, while keeping a distinction between the addresses
            // Previously, $3000-$3FFF was being "redirected" to $2000-$2FFF to avoid MMC3 IRQ issues (which is incorrect)
            // More info here: https://forums.nesdev.com/viewtopic.php?p=132145#p132145
            addPpuMemoryMapping(
                0x3000 + index * 0x400,
                0x3000 + (index + 1) * 0x400 - 1,
                nametableIndex,
                ChrMemoryType.NAMETABLE_RAM,
            )
        } else {
            throw IllegalArgumentException("Invalid nametable index")
        }
    }

    fun nametableAt(nametableIndex: Int): Pointer {
        if (nametableIndex in 0 until NAMETABLE_COUNT) {
            return Pointer(nametableRam, nametableIndex * NAMETABLE_SIZE)
        } else {
            throw IllegalArgumentException("Invalid nametable index")
        }
    }

    fun initialize(console: Console, data: RomData) {
        this.console = console
        this.data = data
        console.mapper = this

        mSaveRamSize = if (data.saveRamSize == -1) if (hasBattery) saveRamSize else 0
        else if (isForceSaveRamSize) saveRamSize
        else data.saveRamSize

        mWorkRamSize = if (data.workRamSize == -1) if (hasBattery) 0 else workRamSize
        else if (isForceWorkRamSize) workRamSize
        else data.workRamSize

        isReadRegisterAddr.fill(false)
        isWriteRegisterAddr.fill(false)

        addRegisterRange(registerStartAddress, registerEndAddress, MemoryOperation.ANY)

        mPrgSize = data.prgRom.size
        mChrRomSize = data.chrRom.size

        prgRom = IntArray(mPrgSize)
        chrRom = IntArray(mChrRomSize)

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

        saveRam = IntArray(mSaveRamSize)
        workRam = IntArray(mWorkRamSize)

        console.initializeRam(saveRam)
        console.initializeRam(workRam)

        nametableRam = IntArray(NAMETABLE_SIZE * NAMETABLE_COUNT)
        console.initializeRam(nametableRam)

        for (i in 0..0xFF) {
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
            mChrRomSize == 0 -> {
                // Assume there is CHR RAM if no CHR ROM exists
                initializeChrRam(data.chrRamSize)
                // Map CHR RAM to 0x0000-0x1FFF by default when no CHR ROM exists
                addPpuMemoryMapping(0x0000, 0x1FFF, 0, ChrMemoryType.RAM)
            }
            data.chrRamSize >= 0 -> {
                initializeChrRam(data.chrRamSize)
            }
            chrRamSize > 0 -> {
                initializeChrRam()
            }
        }

        if (info.hasTreiner) {
            if (mWorkRamSize >= 0x2000) {
                data.treinerData.copyInto(workRam, 0x1000, 0, 512)
            } else if (mSaveRamSize >= 0x2000) {
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

        loadBattery()
    }

    open fun initialize(data: RomData) {
        initialize()
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        if (info.system == GameSystem.VS_SYSTEM) {
            ranges.addHandler(MemoryOperation.READ, 0x6000, 0xFFFF)
            ranges.addHandler(MemoryOperation.WRITE, 0x6000, 0xFFFF)
        } else {
            ranges.addHandler(MemoryOperation.READ, 0x4018, 0xFFFF)
            ranges.addHandler(MemoryOperation.WRITE, 0x4018, 0xFFFF)
        }
    }

    override fun read(addr: Int, type: MemoryOperationType): Int {
        return if (allowRegisterRead && isReadRegisterAddr[addr]) {
            readRegister(addr)
        } else {
            val hi = addr.hiByte

            if (prgMemoryAccess[hi].read) {
                prgPages[hi][addr.loByte]
            } else {
                console.memoryManager.openBus()
            }
        }
    }

    override fun peek(addr: Int): Int {
        val hi = addr.hiByte
        return if (prgMemoryAccess[hi].read) prgPages[hi][addr.loByte]
        else addr shr 8
    }

    protected fun internalRead(addr: Int): Int {
        val hi = addr.hiByte
        val page = prgPages[hi]
        return if (page !== Pointer.NULL) page[addr.loByte] else 0
    }

    fun readVRAM(addr: Int) = mapperReadVRAM(addr)

    open fun mapperReadVRAM(addr: Int) = internalReadVRAM(addr)

    fun internalReadVRAM(addr: Int): Int {
        val hi = addr.hiByte

        if (chrMemoryAccess[hi].read) {
            return chrPages[hi][addr.loByte]
        }

        // Open bus - "When CHR is disabled, the pattern tables are open bus.
        // Theoretically, this should return the LSB of the address read,
        // but real-world behavior varies."
        // return if (vramOpenBusValue >= 0) vramOpenBusValue else addr
        return addr // vramOpenBusValue is always -1 ??
    }

    override fun write(addr: Int, value: Int, type: MemoryOperationType) {
        if (isWriteRegisterAddr[addr]) {
            if (privateHasBusConflicts) {
                val hi = addr.hiByte
                val lo = addr.loByte
                writeRegister(addr, value and prgPages[hi][lo])
            } else {
                writeRegister(addr, value)
            }
        } else {
            writePrgRam(addr, value)
        }
    }

    open fun writePrgRam(addr: Int, value: Int) {
        val hi = addr.hiByte

        if (prgMemoryAccess[hi].write) {
            val page = prgPages[hi]
            page[addr.loByte] = value
        }
    }

    fun writeVRAM(addr: Int, value: Int) {
        val hi = addr.hiByte

        if (chrMemoryAccess[hi].write) {
            val page = chrPages[hi]
            page[addr.loByte] = value
        }
    }

    protected fun validateAddressRange(start: Int, end: Int): Boolean {
        // Start/End address must be multiples of 256/0x100
        return start.loByte == 0 && end.loByte == 0xFF
    }

    private fun setupDefaultWorkRam() {
        // Setup a default work/save ram in 0x6000-0x7FFF space
        if (hasBattery && mSaveRamSize > 0) {
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, PrgMemoryType.SRAM)
        } else if (mWorkRamSize > 0) {
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, PrgMemoryType.WRAM)
        }
    }

    protected fun addCpuMemoryMapping(
        start: Int,
        end: Int,
        pageNumber: Int,
        type: PrgMemoryType,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0xFF00 || end <= start) {
            System.err.println("Invalid CPU address range")
            return
        }

        val pageCount: Int
        val pageSize: Int
        var defaultAccessType = MemoryAccessType.READ

        when (type) {
            PrgMemoryType.ROM -> {
                pageCount = prgPageCount
                pageSize = mPrgPageSize
            }
            PrgMemoryType.SRAM -> {
                pageSize = mSaveRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined save ram.")
                    return
                }

                pageCount = mSaveRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
            else -> {
                pageSize = mWorkRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined work ram.")
                    return
                }

                pageCount = mWorkRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
        }

        if (pageCount == 0) {
            // System.err.println("DEBUG: Tried to map undefined save/work ram.")
            return
        }

        fun wrapPageNumber(page: Int): Int {
            return if (page < 0) {
                // Can't use modulo for negative number because pageCount
                // is sometimes not a power of 2. (Fixes some Mapper 191 games).
                pageCount + page
            } else {
                page % pageCount
            }
        }

        var page = wrapPageNumber(pageNumber)

        if (end - start >= pageSize) {
            // System.err.println("DEBUG: Tried to map undefined prg - page size too small for selected range.")

            var addr = start

            // If range is bigger than a single page, keep going until we reach the last page
            while (addr <= end - pageSize + 1) {
                addCpuMemoryMapping(
                    addr,
                    addr + pageSize - 1,
                    type,
                    page * pageSize,
                    if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType,
                )

                addr += pageSize
                page = wrapPageNumber(page + 1)
            }
        } else {
            addCpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize,
                if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType,
            )
        }
    }

    protected fun addCpuMemoryMapping(
        start: Int,
        end: Int,
        type: PrgMemoryType,
        sourceOffset: Int,
        accessType: MemoryAccessType,
    ) {
        val sourceMemory = when (type) {
            PrgMemoryType.ROM -> Pointer(prgRom)
            PrgMemoryType.SRAM -> Pointer(saveRam)
            else -> Pointer(workRam)
        }

        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        for (i in 0 until slotCount) {
            prgMemoryOffset[firstSlot + i] = sourceOffset + i * 0x100
            prgMemoryType[firstSlot + i] = type
            prgMemoryAccess[firstSlot + i] = accessType
        }

        addCpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun addCpuMemoryMapping(
        start: Int,
        end: Int,
        pointer: Pointer,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start shr 8
        val b = end shr 8
        var source = pointer

        for (i in a..b) {
            prgPages[i] = source

            prgMemoryAccess[i] = if (accessType != MemoryAccessType.UNSPECIFIED) accessType
            else MemoryAccessType.READ

            if (source !== Pointer.NULL) {
                source = Pointer(source, 0x100)
            }
        }
    }

    protected fun removeCpuMemoryMapping(start: Int, end: Int) {
        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        for (i in 0 until slotCount) {
            prgMemoryOffset[firstSlot + i] = -1
            prgMemoryType[firstSlot + i] = PrgMemoryType.ROM
            prgMemoryAccess[firstSlot + i] = MemoryAccessType.NO_ACCESS
        }

        addCpuMemoryMapping(start, end, Pointer.NULL, MemoryAccessType.NO_ACCESS)
    }

    protected fun addPpuMemoryMapping(
        start: Int,
        end: Int,
        pageNumber: Int,
        type: ChrMemoryType,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0x3F00 || end > 0x3FFF || end <= start) {
            System.err.println("Invalid PPU address range")
            return
        }

        val pageCount: Int
        val pageSize: Int
        var defaultAccessType = MemoryAccessType.READ
        var mType = type

        if (mType == ChrMemoryType.DEFAULT) {
            mType = (if (mChrRomSize > 0) ChrMemoryType.ROM else ChrMemoryType.RAM)
        }

        when (mType) {
            ChrMemoryType.ROM -> {
                pageSize = mChrPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined chr rom.")
                    return
                }

                pageCount = chrPageCount
            }
            ChrMemoryType.RAM -> {
                pageSize = mChrRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined chr ram.")
                    return
                }

                pageCount = mChrRamSize / pageSize
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
            else -> {
                pageSize = NAMETABLE_SIZE
                pageCount = NAMETABLE_COUNT
                defaultAccessType = MemoryAccessType.READ_WRITE
            }
        }

        if (pageCount == 0) {
            // System.err.println("DEBUG: Tried to map undefined chr ram/ram.")
            return
        }

        fun wrapPageNumber(page: Int): Int {
            return if (page < 0) {
                // Can't use modulo for negative number because pageCount
                // is sometimes not a power of 2. (Fixes some Mapper 191 games).
                pageCount + page
            } else {
                page % pageCount
            }
        }

        var page = wrapPageNumber(pageNumber)

        if (end - start >= pageSize) {
            var addr = start

            while (addr <= end - pageSize + 1) {
                addPpuMemoryMapping(
                    addr,
                    addr + pageSize - 1,
                    type,
                    page * pageSize,
                    accessType
                )

                addr += pageSize
                page = wrapPageNumber(page + 1)
            }
        } else {
            addPpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize,
                if (accessType == MemoryAccessType.UNSPECIFIED) defaultAccessType else accessType,
            )
        }
    }

    protected fun addPpuMemoryMapping(
        start: Int,
        end: Int,
        type: ChrMemoryType,
        sourceOffset: Int,
        accessType: MemoryAccessType,
    ) {
        var mType = type

        if (mType == ChrMemoryType.DEFAULT) {
            mType = (if (mChrRomSize > 0) ChrMemoryType.ROM else ChrMemoryType.RAM)
        }

        val sourceMemory = when (mType) {
            ChrMemoryType.ROM -> Pointer(chrRom)
            ChrMemoryType.RAM -> Pointer(chrRam)
            else -> Pointer(nametableRam)
        }

        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        for (i in 0 until slotCount) {
            chrMemoryOffset[firstSlot + i] = sourceOffset + i * 256
            chrMemoryType[firstSlot + i] = mType
            chrMemoryAccess[firstSlot + i] = accessType
        }

        addPpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun addPpuMemoryMapping(
        start: Int,
        end: Int,
        pointer: Pointer,
        accessType: MemoryAccessType = MemoryAccessType.UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start shr 8
        val b = end shr 8
        var sourceMemory = pointer

        for (i in a..b) {
            chrPages[i] = sourceMemory
            chrMemoryAccess[i] = if (accessType != MemoryAccessType.UNSPECIFIED) accessType
            else MemoryAccessType.READ_WRITE

            if (sourceMemory !== Pointer.NULL) {
                sourceMemory = Pointer(sourceMemory, 0x100)
            }
        }
    }

    protected fun removePpuMemoryMapping(start: Int, end: Int) {
        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        for (i in 0 until slotCount) {
            chrMemoryOffset[firstSlot + i] = -1
            chrMemoryType[firstSlot + i] = ChrMemoryType.DEFAULT
            chrMemoryAccess[firstSlot + i] = MemoryAccessType.NO_ACCESS
        }

        addPpuMemoryMapping(start, end, Pointer.NULL, MemoryAccessType.NO_ACCESS)
    }

    private fun initializeChrRam(size: Int = -1) {
        val defaultRamSize = if (chrRamSize > 0) chrRamSize else 0x2000
        mChrRamSize = if (size >= 0) size else defaultRamSize

        if (mChrRamSize > 0) {
            chrRam = IntArray(mChrRamSize)
            console.initializeRam(chrRam)
        }
    }

    protected fun addRegisterRange(start: Int, end: Int, operation: MemoryOperation = MemoryOperation.ANY) {
        for (i in start..end) {
            if (operation.read) {
                isReadRegisterAddr[i] = true
            }
            if (operation.write) {
                isWriteRegisterAddr[i] = true
            }
        }
    }

    protected fun removeRegisterRange(start: Int, end: Int, operation: MemoryOperation = MemoryOperation.ANY) {
        for (i in start..end) {
            if (operation.read) {
                isReadRegisterAddr[i] = false
            }
            if (operation.write) {
                isWriteRegisterAddr[i] = false
            }
        }
    }

    open fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        if (mPrgSize < 0x8000 && prgPageSize > mPrgSize) {
            // System.err.println("DEBUG: Total PRG size is smaller than available memory range")
            // Total PRG size is smaller than available memory range, map the entire PRG to all slots
            // i.e same logic as NROM (mapper 0) when PRG is 16kb
            // Needed by "Pyramid" (mapper 79)
            var i = 0

            while (i < 0x8000 / mPrgSize) {
                val start = 0x8000 + i * mPrgSize
                val end = start + mPrgSize - 1
                addCpuMemoryMapping(start, end, 0, memoryType)
                i++
            }
        } else {
            val start = 0x8000 + slot * mPrgPageSize
            val end = start + mPrgPageSize - 1
            addCpuMemoryMapping(start, end, page, memoryType)
        }
    }

    fun selectPrgPage4x(slot: Int, page: Int, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        selectPrgPage2x(slot * 2, page, memoryType)
        selectPrgPage2x(slot * 2 + 1, page + 2, memoryType)
    }

    fun selectPrgPage2x(slot: Int, page: Int, memoryType: PrgMemoryType = PrgMemoryType.ROM) {
        selectPrgPage(slot * 2, page, memoryType)
        selectPrgPage(slot * 2 + 1, page + 1, memoryType)
    }

    open fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        var mType = memoryType

        val pageSize = when (mType) {
            ChrMemoryType.NAMETABLE_RAM -> NAMETABLE_SIZE
            ChrMemoryType.DEFAULT -> if (mChrRomSize > 0) {
                mType = ChrMemoryType.ROM
                mChrPageSize
            } else {
                mType = ChrMemoryType.RAM
                mChrRamPageSize
            }
            ChrMemoryType.RAM -> mChrRamPageSize
            else -> mChrPageSize
        }

        val start = slot * pageSize
        val end = start + pageSize - 1

        addPpuMemoryMapping(start, end, page, mType)
    }

    fun selectChrPage8x(slot: Int, page: Int, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage4x(slot, page, memoryType)
        selectChrPage4x(slot * 2 + 1, page + 4, memoryType)
    }

    fun selectChrPage4x(slot: Int, page: Int, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage2x(slot * 2, page, memoryType)
        selectChrPage2x(slot * 2 + 1, page + 2, memoryType)
    }

    fun selectChrPage2x(slot: Int, page: Int, memoryType: ChrMemoryType = ChrMemoryType.DEFAULT) {
        selectChrPage(slot * 2, page, memoryType)
        selectChrPage(slot * 2 + 1, page + 1, memoryType)
    }

    fun powerOnByte(default: Int = 0): Int {
        return if (console.settings.flag(EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE)) {
            Random.nextInt(256)
        } else {
            default
        }
    }

    open fun notifyVRAMAddressChange(addr: Int) {
        // This is called when the VRAM addr on the PPU memory bus changes
        // Used by MMC3/MMC5/etc
    }

    fun debugReadVRAM(addr: Int, disableSideEffects: Boolean = true): Int {
        val a = addr and 0x3FFF
        if (!disableSideEffects) notifyVRAMAddressChange(a)
        return internalReadVRAM(a)
    }

    open fun applySamples(buffer: ShortArray, sampleCount: Int, volume: Double) {}

    val dipSwitches
        get() = console.settings.dipSwitches and ((1 shl dipSwitchCount) - 1)

    val isNes20
        get() = info.isNes20Header

    override fun saveState(s: Snapshot) {
        s.write("mirroringType", mMirroringType)
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
        mMirroringType = s.readEnum("mirroringType", mMirroringType)
        s.readIntArray("chrRam", chrRam)
        s.readIntArray("workRam", workRam)
        s.readIntArray("saveRam", saveRam)
        s.readIntArray("nametableRam", nametableRam)
        s.readIntArray("prgMemoryOffset", prgMemoryOffset)
        s.readIntArray("chrMemoryOffset", chrMemoryOffset)
        s.readArray("prgMemoryType", prgMemoryType)
        s.readArray("chrMemoryType", chrMemoryType)
        s.readArray("prgMemoryAccess", prgMemoryAccess)
        s.readArray("chrMemoryAccess", chrMemoryAccess)

        restorePrgChrState()
    }

    private fun restorePrgChrState() {
        for (i in 0..0xFF) {
            val startAddr = i shl 8

            if (prgMemoryAccess[i] != MemoryAccessType.NO_ACCESS) {
                addCpuMemoryMapping(
                    startAddr,
                    startAddr + 0xFF,
                    prgMemoryType[i],
                    prgMemoryOffset[i],
                    prgMemoryAccess[i],
                )
            } else {
                removeCpuMemoryMapping(startAddr, startAddr + 0xFF)
            }
        }

        for (i in 0..0x3F) {
            val startAddr = i shl 8

            if (chrMemoryAccess[i] != MemoryAccessType.NO_ACCESS) {
                addPpuMemoryMapping(
                    startAddr,
                    startAddr + 0xFF,
                    chrMemoryType[i],
                    chrMemoryOffset[i],
                    chrMemoryAccess[i],
                )
            } else {
                removePpuMemoryMapping(startAddr, startAddr + 0xFF)
            }
        }
    }

    open val availableFeatures: List<ConsoleFeature> = emptyList()

    fun toAbsoluteAddress(addr: Int): Int {
        val prgAddr = prgPages[addr.hiByte].offset + addr.loByte
        return if (prgAddr in 0 until mPrgSize) prgAddr else -1
    }

    companion object {

        @JvmStatic
        fun initialize(
            console: Console,
            rom: IntArray,
            name: String,
            fdsBios: IntArray = IntArray(0),
        ): Pair<Mapper?, RomData?> {
            val data = RomLoader.load(rom, name, fdsBios)

            if ((data.info.isInDatabase || data.info.isNes20Header) && data.info.inputType != GameInputType.UNSPECIFIED) {
                if (console.settings.flag(EmulationFlag.AUTO_CONFIGURE_INPUT)) {
                    console.settings.initializeInputDevices(data.info.inputType, data.info.system)
                }
            } else if (data.info.isInDatabase) {
                val system = data.info.system
                val isFamicom = (system == GameSystem.FAMICOM || system == GameSystem.FDS || system == GameSystem.DENDY)
                console.settings.consoleType = if (isFamicom) ConsoleType.FAMICOM else ConsoleType.NES
            }

            return fromId(data).also { it.initialize(console, data) } to data
        }

        @JvmStatic
        fun fromId(data: RomData): Mapper {
            return when (val id = data.info.mapperId) {
                0 -> NROM()
                1 -> MMC1()
                2 -> UNROM()
                3 -> CNROM(false)
                4 -> if (data.info.subMapperId == 3) McAcc() else MMC3()
                6 -> Mapper006()
                7 -> AXROM()
                8 -> Mapper008()
                11 -> ColorDreams()
                12 -> Mapper012()
                14 -> Mapper014()
                17 -> Mapper017()
                30 -> UnRom512()
                34 -> {
                    when (val sid = data.info.subMapperId) {
                        // BnROM uses CHR RAM (so no CHR rom in the NES file)
                        0 -> if (data.chrRom.isEmpty()) BnRom() else Nina01()
                        1 -> Nina01()
                        2 -> BnRom()
                        else -> throw IOException("Unsupported mapper $id with submapper $sid")
                    }
                }
                36 -> Txc22000()
                37 -> Mapper037()
                40 -> Mapper040()
                44 -> Mapper044()
                45 -> Mapper045()
                46 -> ColorDreams46()
                47 -> Mapper047()
                49 -> Mapper049()
                52 -> Mapper052()
                63 -> Bmc63()
                74 -> Mapper074()
                76 -> Mapper076()
                79 -> Nina0306(false)
                88 -> Mapper088()
                91 -> Mapper091()
                95 -> Mapper095()
                105 -> Mapper105()
                108 -> Bb()
                111 -> Cheapocabra()
                113 -> Nina0306(true)
                114 -> Mapper114()
                115 -> Mapper115()
                118 -> TxSRom()
                119 -> Mapper119()
                121 -> Mapper121()
                123 -> Mapper123()
                132 -> Txc22211a()
                133 -> Sachen133()
                134 -> Mapper134()
                136 -> Sachen136()
                143 -> Sachen143()
                144 -> Mapper144()
                145 -> Sachen145()
                146 -> Nina0306(false)
                147 -> Sachen147()
                148 -> Sachen148()
                149 -> Sachen149()
                154 -> Mapper154()
                155 -> Mapper155()
                162 -> Waixing162()
                164 -> Waixing164()
                172 -> Txc22211b()
                173 -> Txc22211c()
                177 -> Henggedianzi177()
                178 -> Waixing178()
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
                206 -> Namco108()
                213 -> Mapper213()
                214 -> Mapper214()
                235 -> Bmc235()
                240 -> Mapper240()
                241 -> Mapper241()
                242 -> Mapper242()
                244 -> Mapper244()
                246 -> Mapper246()
                252 -> Waixing252()
                250 -> Mapper250()
                253 -> Mapper253()
                254 -> Mapper254()
                255 -> Bmc255()
                286 -> Bs5()
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
