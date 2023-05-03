package br.tiagohm.nestalgia.core

interface Palette {
    val data: UIntArray

    val size: Int

    val isFullColorPalette: Boolean
}