package br.tiagohm.nestalgia.core

interface MemoryHandler : Memory, Peekable {

    fun memoryRanges(ranges: MemoryRanges)
}
