package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.ControllerType
import br.tiagohm.nestalgia.core.ControllerType.*
import javafx.util.StringConverter

object ControllerTypeStringConverter : StringConverter<ControllerType>() {

    override fun toString(key: ControllerType?) = when (key) {
        NES_CONTROLLER -> "NES Controller"
        FAMICOM_CONTROLLER -> "Famicom Controller"
        FAMICOM_CONTROLLER_P2 -> "Famicom Controller P2"
        NES_ZAPPER -> "Zapper (NES)"
        NES_ARKANOID_CONTROLLER -> "Arkanoid Controller (NES)"
        POWER_PAD_SIDE_A -> "Power Pad (Side A)"
        POWER_PAD_SIDE_B -> "Power Pad (Side B)"
        SUBOR_MOUSE -> "Subor Mouse"
        VIRTUAL_BOY_CONTROLLER -> "Virtual Boy Controller"
        FOUR_SCORE -> "Four Score"
        FAMICOM_ZAPPER -> "Zapper (Famicom)"
        TWO_PLAYER_ADAPTER -> "2-Player Adapter"
        FOUR_PLAYER_ADAPTER -> "4-Player Adapter"
        FAMICOM_ARKANOID_CONTROLLER -> "Arkanoid Controller (Famicom)"
        OEKA_KIDS_TABLET -> "Oeka Kids Tablet"
        FAMILY_TRAINER_MAT_SIDE_A -> "Family Trainer Mat (Side A)"
        FAMILY_TRAINER_MAT_SIDE_B -> "Family Trainer Mat (Side B)"
        KONAMI_HYPER_SHOT -> "Konami Hyper Shot"
        FAMILY_BASIC_KEYBOARD -> "Family Basic Keyboard"
        PARTY_TAP -> "Party Tap"
        PACHINKO -> "Pachinko"
        EXCITING_BOXING -> "Exciting Boxing Punching Bag"
        JISSEN_MAHJONG -> "Jissen Mahjong Controller"
        SUBOR_KEYBOARD -> "Subor Keyboard"
        SNES_MOUSE -> "SNES Mouse"
        BARCODE_BATTLER -> "Barcode Battler"
        HORI_TRACK -> "Hori Track"
        BANDAI_HYPER_SHOT -> "Bandai Hyper Shot"
        ASCII_TURBO_FILE -> "ASCII Turbo File"
        BATTLE_BOX -> "Battle Box"
        BANDAI_MICROPHONE -> "Bandai Microphone"
        DATACH_BARCODE_READER -> "Datach Barcode Reader"
        else -> "None"
    }

    override fun fromString(text: String?) = null
}
