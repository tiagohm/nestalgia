package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface MemoryHandler :
    Memory,
    Peekable {
    fun getMemoryRanges(ranges: MemoryRanges)
}