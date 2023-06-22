package br.tiagohm.nestalgia.core

enum class BandaiMicrophoneButton(override val bit: Int) : ControllerButton {
    A(0),
    B(1),
    MICROPHONE(2);

    override val isCustomKey
        get() = true
}
