package br.tiagohm.nestalgia.core

enum class BandaiMicrophoneButton(override val bit: Int) : ControllerButton, HasCustomKey {
    A(0),
    B(1),
    MICROPHONE(2);

    override val keyIndex = 7 + ordinal
}
