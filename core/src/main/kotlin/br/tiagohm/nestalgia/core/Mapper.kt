package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ChrMemoryType.*
import br.tiagohm.nestalgia.core.EmulationFlag.AUTO_CONFIGURE_INPUT
import br.tiagohm.nestalgia.core.EmulationFlag.RANDOMIZE_MAPPER_POWER_ON_STATE
import br.tiagohm.nestalgia.core.MemoryAccessType.*
import br.tiagohm.nestalgia.core.MemoryOperationType.PPU_RENDERING_READ
import br.tiagohm.nestalgia.core.MirroringType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.*
import br.tiagohm.nestalgia.core.PrgMemoryType.ROM
import org.slf4j.LoggerFactory
import kotlin.math.min
import kotlin.random.Random

// https://wiki.nesdev.com/w/index.php/Mapper

abstract class Mapper(@JvmField protected val console: Console) : Resetable, Battery, Peekable, MemoryHandler, Initializable, Clockable, Snapshotable, AutoCloseable {

    open val prgPageSize = 0

    open val chrPageSize = 0

    open val chrRamSize = 0

    open val internalRamSize = MemoryManager.INTERNAL_RAM_SIZE

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

    @Volatile protected var mSaveRamSize = 0
        private set

    @Volatile protected var mWorkRamSize = 0
        private set

    @Volatile protected var mPrgSize = 0
        private set

    @Volatile protected var mChrRomSize = 0
        private set

    @Volatile protected var mChrRamSize = 0
        private set

    @Volatile protected var onlyChrRam = false
        private set

    @JvmField @Volatile protected var vramOpenBusValue = -1

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

    @PublishedApi @JvmField internal var data = RomData.EMPTY

    inline val info
        get() = data.info

    inline val name
        get() = info.name

    val hasChrRam
        get() = mChrRamSize > 0

    val hasChrRom
        get() = !onlyChrRam

    @JvmField @Volatile protected var prgRom = IntArray(0)
    @JvmField @Volatile protected var chrRom = IntArray(0)
    @JvmField @Volatile protected var chrRam = IntArray(0)
    @JvmField @Volatile protected var saveRam = IntArray(0)
    @JvmField @Volatile protected var workRam = IntArray(0)
    @JvmField @Volatile protected var nametableRam = IntArray(0)

    @Volatile private var hasChrBattery = false
    @Volatile private var mHasBusConflicts = false

    private val prgMemoryAccess = Array(0x100) { NO_ACCESS }
    private val prgPages = Array(0x100) { Pointer.NULL }
    private val prgMemoryOffset = IntArray(0x100)
    private val prgMemoryType = Array(0x100) { ROM }
    private val chrMemoryAccess = Array(0x100) { NO_ACCESS }
    private val chrPages = Array(0x100) { Pointer.NULL }
    private val chrMemoryOffset = IntArray(0x100)
    private val chrMemoryType = Array(0x100) { DEFAULT }

    override fun close() = Unit

    override fun reset(softReset: Boolean) = Unit

    protected open fun writeRegister(addr: Int, value: Int) = Unit

    protected open fun readRegister(addr: Int) = 0

    open fun updateRegion(region: Region) = Unit

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
            console.batteryManager.loadBattery(".sav", mSaveRamSize).copyInto(saveRam)
        }
        if (hasChrBattery && mChrRamSize > 0) {
            console.batteryManager.loadBattery(".sav.chr", mChrRamSize).copyInto(chrRam)
        }
    }

    override fun clock() = Unit

    fun copyPrgChrRom(mapper: Mapper) {
        if (mPrgSize == mapper.mPrgSize &&
            mChrRomSize == mapper.mChrRomSize
        ) {
            mapper.prgRom.copyInto(prgRom, 0, 0, mPrgSize)

            if (!onlyChrRam) {
                mapper.chrRom.copyInto(chrRom, 0, 0, mChrRomSize)
            }
        }
    }

    @Volatile private var mMirroringType = HORIZONTAL

    var mirroringType: MirroringType
        get() = mMirroringType
        set(value) {
            mMirroringType = value

            when (value) {
                VERTICAL -> nametables(0, 1, 0, 1)
                HORIZONTAL -> nametables(0, 0, 1, 1)
                FOUR_SCREENS -> nametables(0, 1, 2, 3)
                SCREEN_A_ONLY -> nametables(0, 0, 0, 0)
                SCREEN_B_ONLY -> nametables(1, 1, 1, 1)
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
                NAMETABLE_RAM,
            )
            // Mirror $2000-$2FFF to $3000-$3FFF, while keeping a distinction between the addresses
            // Previously, $3000-$3FFF was being "redirected" to $2000-$2FFF to avoid MMC3 IRQ issues (which is incorrect)
            // More info here: https://forums.nesdev.com/viewtopic.php?p=132145#p132145
            addPpuMemoryMapping(
                0x3000 + index * 0x400,
                0x3000 + (index + 1) * 0x400 - 1,
                nametableIndex,
                NAMETABLE_RAM,
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

    fun initialize(data: RomData) {
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

        addRegisterRange(registerStartAddress, registerEndAddress, READ_WRITE)

        mPrgSize = data.prgRom.size
        mChrRomSize = data.chrRom.size

        prgRom = IntArray(mPrgSize)
        chrRom = IntArray(mChrRomSize)

        data.prgRom.copyInto(prgRom)
        data.chrRom.copyInto(chrRom)

        hasChrBattery = data.saveChrRamSize > 0 || isForceChrBattery

        mHasBusConflicts = when (data.info.busConflict) {
            BusConflictType.DEFAULT -> hasBusConflicts
            BusConflictType.YES -> true
            BusConflictType.NO -> false
        }

        saveRam = IntArray(mSaveRamSize)
        workRam = IntArray(mWorkRamSize)

        console.initializeRam(saveRam)
        console.initializeRam(workRam)

        nametableRam = IntArray(NAMETABLE_SIZE * NAMETABLE_COUNT)
        console.initializeRam(nametableRam)

        repeat(256) {
            // Allow us to map a different page every 256 bytes
            prgPages[it] = Pointer.NULL
            prgMemoryOffset[it] = -1
            prgMemoryType[it] = ROM
            prgMemoryAccess[it] = NO_ACCESS

            chrPages[it] = Pointer.NULL
            chrMemoryOffset[it] = -1
            chrMemoryType[it] = DEFAULT
            chrMemoryAccess[it] = NO_ACCESS
        }

        when {
            mChrRomSize == 0 -> {
                // Assume there is CHR RAM if no CHR ROM exists.
                onlyChrRam = true
                initializeChrRam(data.chrRamSize)
                // Map CHR RAM to 0x0000-0x1FFF by default when no CHR ROM exists.
                addPpuMemoryMapping(0x0000, 0x1FFF, 0, RAM)
                mChrRomSize = mChrRamSize
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
            busConflict = if (mHasBusConflicts) BusConflictType.YES else BusConflictType.NO,
        )

        this.data = data.copy(info = info)

        loadBattery()

        LOG.info(
            "[{}]: sram={} wram={} chrRom={} chrRam={} mirroringType={} battery={} chrBattery={} busConflict={}",
            name, mSaveRamSize, mWorkRamSize, mChrRomSize, chrRamSize, mirroringType, hasBattery, hasChrBattery, mHasBusConflicts
        )
    }

    override fun memoryRanges(ranges: MemoryRanges) {
        if (info.system == GameSystem.VS_SYSTEM) {
            ranges.addHandler(READ, 0x6000, 0xFFFF)
            ranges.addHandler(WRITE, 0x6000, 0xFFFF)
        } else {
            ranges.addHandler(READ, 0x4018, 0xFFFF)
            ranges.addHandler(WRITE, 0x4018, 0xFFFF)
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

    open fun readVRAM(addr: Int, type: MemoryOperationType = PPU_RENDERING_READ) = internalReadVRAM(addr)

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
            if (mHasBusConflicts) {
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
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, SRAM)
        } else if (mWorkRamSize > 0) {
            addCpuMemoryMapping(0x6000, 0x7FFF, 0, WRAM)
        }
    }

    protected fun addCpuMemoryMapping(
        start: Int,
        end: Int,
        pageNumber: Int,
        type: PrgMemoryType,
        accessType: MemoryAccessType = UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0xFF00 || end <= start) {
            LOG.error("invalid CPU address range. start={}, end={}", start, end)
            return
        }

        val pageCount: Int
        val pageSize: Int
        var defaultAccessType = READ

        when (type) {
            ROM -> {
                pageCount = prgPageCount
                pageSize = mPrgPageSize
            }
            SRAM -> {
                pageSize = mSaveRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined save ram.")
                    return
                }

                pageCount = mSaveRamSize / pageSize
                defaultAccessType = READ_WRITE
            }
            else -> {
                pageSize = mWorkRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined work ram.")
                    return
                }

                pageCount = mWorkRamSize / pageSize
                defaultAccessType = READ_WRITE
            }
        }

        if (pageCount == 0) {
            // System.err.println("DEBUG: Tried to map undefined save/work ram.")
            return
        }

        var page = wrapPageNumber(pageNumber, pageCount)

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
                    if (accessType == UNSPECIFIED) defaultAccessType else accessType,
                )

                addr += pageSize
                page = wrapPageNumber(page + 1, pageCount)
            }
        } else {
            addCpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize,
                if (accessType == UNSPECIFIED) defaultAccessType else accessType,
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
            ROM -> Pointer(prgRom)
            SRAM -> Pointer(saveRam)
            else -> Pointer(workRam)
        }

        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        repeat(slotCount) {
            prgMemoryOffset[firstSlot + it] = sourceOffset + it * 0x100
            prgMemoryType[firstSlot + it] = type
            prgMemoryAccess[firstSlot + it] = accessType
        }

        addCpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun addCpuMemoryMapping(
        start: Int,
        end: Int,
        pointer: Pointer,
        accessType: MemoryAccessType = UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start shr 8
        val b = end shr 8
        var source = pointer

        for (i in a..b) {
            prgPages[i] = source
            prgMemoryAccess[i] = if (accessType != UNSPECIFIED) accessType else READ

            if (source !== Pointer.NULL) {
                source = Pointer(source, 0x100)
            }
        }
    }

    protected fun removeCpuMemoryMapping(start: Int, end: Int) {
        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        repeat(slotCount) {
            prgMemoryOffset[firstSlot + it] = -1
            prgMemoryType[firstSlot + it] = ROM
            prgMemoryAccess[firstSlot + it] = NO_ACCESS
        }

        addCpuMemoryMapping(start, end, Pointer.NULL, NO_ACCESS)
    }

    protected fun addPpuMemoryMapping(
        start: Int,
        end: Int,
        pageNumber: Int,
        type: ChrMemoryType = DEFAULT,
        accessType: MemoryAccessType = UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end) || start > 0x3F00 || end > 0x3FFF || end <= start) {
            LOG.error("invalid PPU address range. start=%04X, end=%04X".format(start, end))
            return
        }

        val pageCount: Int
        val pageSize: Int
        var defaultAccessType = READ

        when (type) {
            DEFAULT -> {
                pageSize = mChrPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined chr rom/ram.")
                    return
                }

                pageCount = chrPageCount

                if (onlyChrRam) {
                    defaultAccessType = READ_WRITE
                }
            }
            ChrMemoryType.ROM -> {
                pageSize = mChrPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined chr rom.")
                    return
                }

                pageCount = chrPageCount
            }
            RAM -> {
                pageSize = mChrRamPageSize

                if (pageSize == 0) {
                    // System.err.println("DEBUG: Tried to map undefined chr ram.")
                    return
                }

                pageCount = mChrRamSize / pageSize
                defaultAccessType = READ_WRITE
            }
            else -> {
                pageSize = NAMETABLE_SIZE
                pageCount = NAMETABLE_COUNT
                defaultAccessType = READ_WRITE
            }
        }

        if (pageCount == 0) {
            // System.err.println("DEBUG: Tried to map undefined chr ram/ram.")
            return
        }

        var page = wrapPageNumber(pageNumber, pageCount)

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
                page = wrapPageNumber(page + 1, pageCount)
            }
        } else {
            addPpuMemoryMapping(
                start,
                end,
                type,
                page * pageSize,
                if (accessType == UNSPECIFIED) defaultAccessType else accessType,
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
        var sourceType = type

        val sourceMemory = when (sourceType) {
            DEFAULT -> {
                sourceType = if (onlyChrRam) RAM else ChrMemoryType.ROM
                if (onlyChrRam) Pointer(chrRam) else Pointer(chrRom)
            }
            ChrMemoryType.ROM -> Pointer(chrRom)
            RAM -> Pointer(chrRam)
            else -> Pointer(nametableRam)
        }

        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        repeat(slotCount) {
            chrMemoryOffset[firstSlot + it] = sourceOffset + it * 256
            chrMemoryType[firstSlot + it] = sourceType
            chrMemoryAccess[firstSlot + it] = accessType
        }

        addPpuMemoryMapping(start, end, Pointer(sourceMemory, sourceOffset), accessType)
    }

    protected fun addPpuMemoryMapping(
        start: Int,
        end: Int,
        pointer: Pointer,
        accessType: MemoryAccessType = UNSPECIFIED,
    ) {
        if (!validateAddressRange(start, end)) {
            return
        }

        val a = start shr 8
        val b = end shr 8
        var sourceMemory = pointer

        for (i in a..b) {
            chrPages[i] = sourceMemory
            chrMemoryAccess[i] = if (accessType != UNSPECIFIED) accessType else READ_WRITE

            if (sourceMemory !== Pointer.NULL) {
                sourceMemory = Pointer(sourceMemory, 0x100)
            }
        }
    }

    protected fun removePpuMemoryMapping(start: Int, end: Int) {
        val firstSlot = start shr 8
        val slotCount = (end - start + 1) shr 8

        // Unmap this section of memory (causing open bus behavior)
        repeat(slotCount) {
            chrMemoryOffset[firstSlot + it] = -1
            chrMemoryType[firstSlot + it] = DEFAULT
            chrMemoryAccess[firstSlot + it] = NO_ACCESS
        }

        addPpuMemoryMapping(start, end, Pointer.NULL, NO_ACCESS)
    }

    private fun initializeChrRam(size: Int = -1) {
        val defaultRamSize = if (chrRamSize > 0) chrRamSize else 0x2000
        mChrRamSize = if (size >= 0) size else defaultRamSize

        if (mChrRamSize > 0) {
            chrRam = IntArray(mChrRamSize)
            console.initializeRam(chrRam)
        }
    }

    protected fun addRegisterRange(start: Int, end: Int, operation: MemoryAccessType) {
        for (i in start..end) {
            if (operation.read) {
                isReadRegisterAddr[i] = true
            }
            if (operation.write) {
                isWriteRegisterAddr[i] = true
            }
        }
    }

    protected fun removeRegisterRange(start: Int, end: Int, operation: MemoryAccessType) {
        for (i in start..end) {
            if (operation.read) {
                isReadRegisterAddr[i] = false
            }
            if (operation.write) {
                isWriteRegisterAddr[i] = false
            }
        }
    }

    open fun selectPrgPage(slot: Int, page: Int, memoryType: PrgMemoryType = ROM) {
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

    fun selectPrgPage4x(slot: Int, page: Int, memoryType: PrgMemoryType = ROM) {
        selectPrgPage2x(slot * 2, page, memoryType)
        selectPrgPage2x(slot * 2 + 1, page + 2, memoryType)
    }

    fun selectPrgPage2x(slot: Int, page: Int, memoryType: PrgMemoryType = ROM) {
        selectPrgPage(slot * 2, page, memoryType)
        selectPrgPage(slot * 2 + 1, page + 1, memoryType)
    }

    open fun selectChrPage(slot: Int, page: Int, memoryType: ChrMemoryType = DEFAULT) {
        val pageSize = when (memoryType) {
            NAMETABLE_RAM -> NAMETABLE_SIZE
            RAM -> mChrRamPageSize
            else -> mChrPageSize
        }

        val start = slot * pageSize
        val end = start + pageSize - 1

        addPpuMemoryMapping(start, end, page, memoryType)
    }

    fun selectChrPage8x(slot: Int, page: Int, memoryType: ChrMemoryType = DEFAULT) {
        selectChrPage4x(slot, page, memoryType)
        selectChrPage4x(slot * 2 + 1, page + 4, memoryType)
    }

    fun selectChrPage4x(slot: Int, page: Int, memoryType: ChrMemoryType = DEFAULT) {
        selectChrPage2x(slot * 2, page, memoryType)
        selectChrPage2x(slot * 2 + 1, page + 2, memoryType)
    }

    fun selectChrPage2x(slot: Int, page: Int, memoryType: ChrMemoryType = DEFAULT) {
        selectChrPage(slot * 2, page, memoryType)
        selectChrPage(slot * 2 + 1, page + 1, memoryType)
    }

    fun powerOnByte(default: Int = 0): Int {
        return if (console.settings.flag(RANDOMIZE_MAPPER_POWER_ON_STATE)) Random.nextInt(256)
        else default
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

    fun debugRead(addr: Int): Int {
        val hi = addr shr 8

        if (prgMemoryAccess[hi] == READ) {
            val page = prgPages[hi]
            return page[addr and 0xFF]
        }

        // Fake open bus.
        return addr shr 8
    }

    open fun applySamples(buffer: ShortArray, sampleCount: Int, volume: Double) = Unit

    val dipSwitches
        get() = console.settings.dipSwitches and ((1 shl dipSwitchCount) - 1)

    inline val isNes20
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
        repeat(256) {
            val startAddr = it shl 8

            if (prgMemoryAccess[it] != NO_ACCESS) {
                addCpuMemoryMapping(
                    startAddr,
                    startAddr + 0xFF,
                    prgMemoryType[it],
                    prgMemoryOffset[it],
                    prgMemoryAccess[it],
                )
            } else {
                removeCpuMemoryMapping(startAddr, startAddr + 0xFF)
            }
        }

        repeat(64) {
            val startAddr = it shl 8

            if (chrMemoryAccess[it] != NO_ACCESS) {
                addPpuMemoryMapping(
                    startAddr,
                    startAddr + 0xFF,
                    chrMemoryType[it],
                    chrMemoryOffset[it],
                    chrMemoryAccess[it],
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

        private val LOG = LoggerFactory.getLogger(Mapper::class.java)

        private fun wrapPageNumber(page: Int, pageCount: Int): Int {
            // Can't use modulo for negative number because pageCount
            // is sometimes not a power of 2. (Fixes some Mapper 191 games).
            return if (page < 0) pageCount + page else page % pageCount
        }

        fun initialize(
            console: Console,
            rom: ByteArray,
            name: String,
            fdsBios: ByteArray = ByteArray(0),
        ): Mapper {
            val data = CompressedRomLoader.load(rom, name, fdsBios)

            if ((data.info.isInDatabase || data.info.isNes20Header) && data.info.inputType != GameInputType.UNSPECIFIED) {
                if (console.settings.flag(AUTO_CONFIGURE_INPUT)) {
                    console.settings.initializeInputDevices(data.info.inputType, data.info.system)
                }
            }

            return MapperFactory.from(console, data)
                .also { it.initialize(data) }
        }

        const val NAMETABLE_COUNT = 0x10
        const val NAMETABLE_SIZE = 0x400
    }
}
