package br.tiagohm.nestalgia.core

sealed interface MemoryHandler : Memory, Peekable {

    fun memoryRanges(ranges: MemoryRanges)
}
