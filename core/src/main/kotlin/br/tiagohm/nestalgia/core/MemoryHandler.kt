package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
interface MemoryHandler : Memory {
    fun getMemoryRanges(ranges: MemoryRanges)
}