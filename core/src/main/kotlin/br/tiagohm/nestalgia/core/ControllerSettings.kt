package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.ControllerType.*

data class ControllerSettings(
    @JvmField val keyMapping: KeyMapping = KeyMapping(),
    @JvmField var type: ControllerType = NONE,
) : Snapshotable {

    fun populateKeyMappingWithDefault(): Boolean {
        val defaultKeyMapping = when (type) {
            HORI_TRACK,
            NES_CONTROLLER,
            FAMICOM_CONTROLLER,
            FAMICOM_CONTROLLER_P2 -> StandardController.defaultKeyMapping()
            NES_ZAPPER,
            FAMICOM_ZAPPER -> Zapper.defaultKeyMapping()
            NES_ARKANOID_CONTROLLER,
            FAMICOM_ARKANOID_CONTROLLER -> ArkanoidController.defaultKeyMapping()
            FAMILY_TRAINER_MAT_SIDE_A,
            FAMILY_TRAINER_MAT_SIDE_B,
            POWER_PAD_SIDE_A,
            POWER_PAD_SIDE_B -> PowerPad.defaultKeyMapping()
            KONAMI_HYPER_SHOT -> KonamiHyperShot.defaultKeyMapping()
            PARTY_TAP -> PartyTap.defaultKeyMapping()
            PACHINKO -> Pachinko.defaultKeyMapping()
            EXCITING_BOXING -> ExcitingBoxingController.defaultKeyMapping()
            JISSEN_MAHJONG -> JissenMahjong.defaultKeyMapping()
            BANDAI_HYPER_SHOT -> BandaiHyperShot.defaultKeyMapping()
            SUBOR_MOUSE,
            VIRTUAL_BOY_CONTROLLER,
            FOUR_SCORE,
            TWO_PLAYER_ADAPTER,
            FOUR_PLAYER_ADAPTER,
            OEKA_KIDS_TABLET,
            FAMILY_BASIC_KEYBOARD,
            SUBOR_KEYBOARD,
            SNES_MOUSE,
            BARCODE_BATTLER,
            ASCII_TURBO_FILE,
            BATTLE_BOX,
            BANDAI_MICROPHONE,
            DATACH_BARCODE_READER,
            NONE -> return false
        }

        return defaultKeyMapping.copyIntoIfUndefined(keyMapping)
    }

    override fun saveState(s: Snapshot) {
        s.write("keyMapping", keyMapping)
        s.write("type", type)
    }

    override fun restoreState(s: Snapshot) {
        s.readSnapshotable("keyMapping", keyMapping, keyMapping::reset)
        type = s.readEnum("type", NONE)
    }
}
