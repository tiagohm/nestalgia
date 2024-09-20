package br.tiagohm.nestalgia.desktop.gui.converters

import br.tiagohm.nestalgia.core.*
import javafx.util.StringConverter

object ControllerButtonStringConverter : StringConverter<ControllerButton>() {

    override fun toString(key: ControllerButton?) = if (key == null) "-" else LABELS[key] ?: "$key"

    override fun fromString(text: String?) = null

    private val LABELS = mapOf<ControllerButton, String>(
        StandardController.Button.A to "A",
        StandardController.Button.B to "B",
        StandardController.Button.UP to "Up",
        StandardController.Button.LEFT to "Left",
        StandardController.Button.RIGHT to "Right",
        StandardController.Button.DOWN to "Down",
        StandardController.Button.START to "Start",
        StandardController.Button.SELECT to "Select",
        StandardController.Button.MICROPHONE to "Microphone",
        StandardController.Button.TURBO_A to "Turbo (A)",
        StandardController.Button.TURBO_B to "Turbo (B)",
        Zapper.Button.FIRE to "Fire",
        ArkanoidController.Button.FIRE to "Fire",
        BandaiHyperShot.Button.FIRE to "Fire",
        ExcitingBoxingController.Button.HIT_BODY to "Body",
        ExcitingBoxingController.Button.HOOK_LEFT to "Hook (left)",
        ExcitingBoxingController.Button.HOOK_RIGHT to "Hook (right)",
        ExcitingBoxingController.Button.JAB_LEFT to "Jab (left)",
        ExcitingBoxingController.Button.JAB_RIGHT to "Jab (right)",
        ExcitingBoxingController.Button.MOVE_LEFT to "Move (left)",
        ExcitingBoxingController.Button.MOVE_RIGHT to "Move (right)",
        ExcitingBoxingController.Button.STRAIGHT to "Straight",
        KonamiHyperShot.Button.RUN_P1 to "Run (player 1)",
        KonamiHyperShot.Button.RUN_P2 to "Run (player 2)",
        KonamiHyperShot.Button.JUMP_P1 to "Jump (player 1)",
        KonamiHyperShot.Button.JUMP_P2 to "Jump (player 2)",
        Pachinko.Button.PRESS to "Press",
        Pachinko.Button.RELEASE to "Release",
        JissenMahjong.Button.SELECT to "Select",
        JissenMahjong.Button.START to "Start",
        JissenMahjong.Button.KAN to "Kan",
        JissenMahjong.Button.PON to "Pon",
        JissenMahjong.Button.CHII to "Chii",
        JissenMahjong.Button.RIICHI to "Riichi",
        JissenMahjong.Button.RON to "Ron",
    )
}
