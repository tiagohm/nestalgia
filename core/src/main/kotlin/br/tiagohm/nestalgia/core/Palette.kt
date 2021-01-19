package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface Palette {
    val data: UIntArray

    val size: Int

    val isFullColorPalette: Boolean
}