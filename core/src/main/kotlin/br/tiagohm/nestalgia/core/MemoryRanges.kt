package br.tiagohm.nestalgia.core

import java.util.*

class MemoryRanges {

    val ramReadAddresses = ArrayList<UShort>()
    val ramWriteAddresses = ArrayList<UShort>()
    var allowOverride = false

    fun addHandler(operation: MemoryOperation, start: UShort, end: UShort = 0U) {
        val b = if (end.toUInt() == 0U) start else end

        if (operation.isRead) {
            for (i in start..b) {
                ramReadAddresses.add(i.toUShort())
            }
        }

        if (operation.isWrite) {
            for (i in start..b) {
                ramWriteAddresses.add(i.toUShort())
            }
        }
    }
}
