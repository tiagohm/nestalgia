package br.tiagohm.nestalgia.core

data class PageInfo(
    @JvmField val leadInOffset: Int,
    @JvmField val audioOffset: Int,
    @JvmField val data: ByteArray,
)
