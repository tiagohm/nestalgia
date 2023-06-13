package br.tiagohm.nestalgia.core

data class CodeInfo(
    @JvmField val address: Int,
    @JvmField val value: Int,
    @JvmField val compareValue: Int,
    @JvmField val isRelativeAddress: Boolean,
)
