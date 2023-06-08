package br.tiagohm.nestalgia.core

interface Palette {

    val data: IntArray

    val size
        get() = data.size

    val isFullColor
        get() = size == 512
}
