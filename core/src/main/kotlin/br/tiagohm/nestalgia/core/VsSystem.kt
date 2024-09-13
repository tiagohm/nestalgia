package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.GameSystem.*
import br.tiagohm.nestalgia.core.VsSystemType.*

// https://wiki.nesdev.com/w/index.php/INES_Mapper_099

class VsSystem(console: Console) : Mapper(console) {

    override val prgPageSize = 0x2000

    override val chrPageSize = 0x2000

    override val workRamSize = 0x800

    private val isVsMainConsole = true
    @Volatile private var vsControlManager: VsControlManager? = null

    override fun initialize() {
        if (!isNes20) {
            // Force VS system if mapper 99.
            info.system = VS_SYSTEM

            if (mPrgSize >= 0x10000) {
                info.vsType = VS_DUAL_SYSTEM
            } else {
                info.vsType = DEFAULT
            }
        }

        // Unlike all other mappers, an undersize mapper 99 image implies
        // open bus instead of mirroring.
        // However, it doesn't look like any game actually rely on this behavior?
        // So not implemented for now.
        var initialized = false

        if (mPrgSize == 0xC000) {
            // 48KB rom == unpadded dualsystem rom
            if (info.vsType == VS_DUAL_SYSTEM) {
                val prgOuter = if (isVsMainConsole) 0 else 3

                selectPrgPage(1, 0 + prgOuter)
                selectPrgPage(2, 1 + prgOuter)
                selectPrgPage(3, 2 + prgOuter)
                initialized = true
            } else if (info.vsType == RAID_ON_BUNGELING_BAY_PROTECTION) {
                if (isVsMainConsole) {
                    selectPrgPage(0, 0)
                    selectPrgPage(1, 1)
                    selectPrgPage(2, 2)
                    selectPrgPage(3, 3)
                } else {
                    selectPrgPage(0, 4)
                }

                initialized = true
            }
        }

        if (!initialized) {
            val prgOuter = if (isVsMainConsole) 0 else 4
            selectPrgPage(0, 0 or prgOuter)
            selectPrgPage(1, 1 or prgOuter)
            selectPrgPage(2, 2 or prgOuter)
            selectPrgPage(3, 3 or prgOuter)
        }

        val chrOuter = if (isVsMainConsole) 0 else 2
        selectChrPage(0, 0 or chrOuter)

        vsControlManager = console.controlManager as VsControlManager
    }
}
