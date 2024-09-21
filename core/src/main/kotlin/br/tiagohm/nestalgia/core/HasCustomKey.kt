package br.tiagohm.nestalgia.core

// Zapper: 0
// Arkanoid: 1
// Bandai Hyper Shot: 2
// Konami Hyper Shot: 3-6
// Bandai Microphone: 7-9
// Exciting Boxing Punching Bag: 10-17
// Power Pad: 18-29
// Pachinko: 30-31
// Party Tap: 32-37
// Jissen Mahjong: 38-58
// Subor Mouse: 59-60
// System Action: 93-94
// FDS: 95-96
// VS System: 97-99
// Subor Keyboard: 100-199
// Bandai Hyper Shot Aim Offscreen: 254
// Zapper Aim Offscreen: 255

sealed interface HasCustomKey {

    val keyIndex: Int
}
