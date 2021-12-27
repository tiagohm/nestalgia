package br.tiagohm.nestalgia.core

interface MemoryHandler :
    Memory,
    Peekable {
    fun getMemoryRanges(ranges: MemoryRanges)
}