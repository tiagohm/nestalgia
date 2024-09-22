package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.HORIZONTAL
import br.tiagohm.nestalgia.core.MirroringType.VERTICAL

// BMC-RESETNROM-XIN1 (Mapper 343)
// submapper 1 - Sheng Tian 2-in-1(Unl,ResetBase)[p1] - Kung Fu (Spartan X), Super Mario Bros (alt)
// submapper 1 - Sheng Tian 2-in-1(Unl,ResetBase)[p2] - B-Wings, Twin-beeSMB1 (wrong mirroring)

class BmcResetNromX1n1(console: Console) : Mapper(console) {

    override val prgPageSize = 0x4000

    override val chrPageSize = 0x2000

    @Volatile private var game = 0
    @Volatile private var subMapperId = 0

    override fun initialize() {
        subMapperId = data.info.subMapperId

        if (!data.info.isNes20Header || !data.info.isInDatabase) {
            if (data.info.hash.crc32 == 0x3470F395L ||    // Sheng Tian 2-in-1(Unl,ResetBase)[p1].unf
                data.info.hash.crc32 == 0x39F9140FL
            ) {
                // Sheng Tian 2-in-1(Unl,ResetBase)[p2].unf
                subMapperId = 1
            }
        }
    }

    override fun reset(softReset: Boolean) {
        if (!softReset) {
            game = 0
        }

        updateState()
    }

    private fun updateState() {
        if (subMapperId == 1) {
            selectPrgPage2x(0, game shl 1)
        } else {
            selectPrgPage(0, game)
            selectPrgPage(1, game)
        }

        selectChrPage(0, game)
        mirroringType = if (game.bit7) VERTICAL else HORIZONTAL
    }

    override fun writeRegister(addr: Int, value: Int) {
        game = value.inv() and 0xFF
        updateState()
    }

    override fun saveState(s: Snapshot) {
        super.saveState(s)

        s.write("game", game)
    }

    override fun restoreState(s: Snapshot) {
        super.restoreState(s)

        game = s.readInt("game")
    }
}
