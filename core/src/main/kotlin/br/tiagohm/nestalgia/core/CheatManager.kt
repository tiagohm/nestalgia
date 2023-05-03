package br.tiagohm.nestalgia.core

import java.util.*

class CheatManager(val console: Console) {

    private val relativeCheatCodes = Array<MutableList<CodeInfo>?>(65536) { null }
    private val absoluteCheatCodes = ArrayList<CodeInfo>()
    private var hasCodes = false

    private fun decode(code: Int, bitIndexes: IntArray): Int {
        var result = 0

        for (i in bitIndexes) {
            result = result shl 1
            result = result or ((code shr i) and 0x01)
        }

        return result
    }

    private fun decodeGameGenie(code: String): CodeInfo {
        var rawCode = 0

        for (i in code.indices) {
            rawCode = rawCode or (GAME_GENIE_LETTERS.indexOf(code[i]) shl (i * 4))
        }

        // Bit 5 of the value is stored in a different location for 8-character codes
        val valueBits = if (code.length == 8) GAME_GENIE_VALUE_BITS_8 else GAME_GENIE_VALUE_BITS

        val address = decode(rawCode, GAME_GENIE_ADDRESS_BITS) + 0x8000
        val value = decode(rawCode, valueBits)
        val compareValue = if (code.length == 8) decode(rawCode, GAME_GENIE_COMPARE_VALUE_BITS) else -1

        return CodeInfo(address, value, compareValue, true)
    }

    private fun decodePAR(code: Int): CodeInfo {
        var key = 0x7E5EE93A
        val xorValue = 0x5C184B91
        // Throw away bit 0, not used.
        var c = code shr 1
        var result = 0

        for (i in 30 downTo 0) {
            if ((((key xor c) shr 30) and 0x01) != 0) {
                result = result or (1 shl PAR_SHIFT_VALUES[i])
                key = key xor xorValue
            }

            c = c shl 1
            key = key shl 1
        }

        val address = (result and 0x7FFF) + 0x8000
        val value = (result shr 24) and 0xFF
        val compareValue = (result shr 16) and 0xFF

        return CodeInfo(address, value, compareValue, true)
    }

    fun addCode(code: CodeInfo) {
        if (code.isRelativeAddress) {
            if (code.address > 0xFFFF) {
                System.err.println("Invalid cheat, ignore it: ${code.address}")
                return
            }

            if (relativeCheatCodes[code.address] == null) {
                relativeCheatCodes[code.address] = ArrayList(1)
            }

            relativeCheatCodes[code.address]!!.add(code)
        } else {
            absoluteCheatCodes.add(code)
        }

        hasCodes = true

        console.notificationManager.sendNotification(NotificationType.CHEAT_ADDED, code)
    }

    fun addGameGenieCode(code: String) {
        addCode(decodeGameGenie(code))
    }

    fun addProActionRockyCode(code: Int) {
        addCode(decodePAR(code))
    }

    fun addCheat(cheat: CheatInfo) {
        when (cheat.type) {
            CheatType.GAME_GENIE -> {
                addGameGenieCode(cheat.gameGenieCode!!)
            }
            CheatType.PRO_ACTION_ROCKY -> {
                addProActionRockyCode(cheat.proActionRockyCode!!)
            }
            else -> {
                addCode(CodeInfo(cheat.address!!, cheat.value!!, cheat.compareValue, cheat.isRelativeAddress))
            }
        }
    }

    fun setCheats(cheats: Iterable<CheatInfo>) {
        console.pause()
        clear()
        cheats.forEach { addCheat(it) }
        console.resume()
    }

    fun clear() {
        if (hasCodes) {
            relativeCheatCodes.forEach { it?.clear() }
            relativeCheatCodes.fill(null)
            absoluteCheatCodes.clear()

            hasCodes = false

            console.notificationManager.sendNotification(NotificationType.CHEAT_REMOVED)
        }
    }

    fun applyCode(addr: UShort, value: UByte): UByte {
        if (hasCodes) {
            if (relativeCheatCodes[addr.toInt()] != null) {
                val codes = relativeCheatCodes[addr.toInt()]!!

                for (code in codes) {
                    if (code.compareValue == -1 || code.compareValue == value.toInt()) {
                        return code.value.toUByte()
                    }
                }
            } else if (absoluteCheatCodes.isNotEmpty()) {
                val absoluteAddr = console.mapper!!.toAbsoluteAddress(addr)

                if (absoluteAddr >= 0) {
                    for (code in absoluteCheatCodes) {
                        if (code.address == absoluteAddr &&
                            (code.compareValue == -1 || code.compareValue == value.toInt())
                        ) {
                            return code.value.toUByte()
                        }
                    }
                }
            }
        }

        return value
    }

    var cheats: List<CodeInfo>
        get() {
            val res = ArrayList<CodeInfo>()
            relativeCheatCodes.forEach { if (it != null) res.addAll(it) }
            res.addAll(absoluteCheatCodes)
            return res
        }
        set(value) {
            clear()
            value.forEach { addCode(it) }
        }

    companion object {
        // Game Genie
        private const val GAME_GENIE_LETTERS = "APZLGITYEOXUKSVN"
        private val GAME_GENIE_ADDRESS_BITS = intArrayOf(14, 13, 12, 19, 22, 21, 20, 7, 10, 9, 8, 15, 18, 17, 16)
        private val GAME_GENIE_VALUE_BITS = intArrayOf(3, 6, 5, 4, 23, 2, 1, 0)
        private val GAME_GENIE_VALUE_BITS_8 = intArrayOf(3, 6, 5, 4, 31, 2, 1, 0)
        private val GAME_GENIE_COMPARE_VALUE_BITS = intArrayOf(27, 30, 29, 28, 23, 26, 25, 24)

        // PAR
        private val PAR_SHIFT_VALUES = intArrayOf(
            3, 13, 14, 1, 6, 9, 5, 0, 12, 7, 2, 8, 10, 11, 4, // Address
            19, 21, 23, 22, 20, 17, 16, 18, // Compare
            29, 31, 24, 26, 25, 30, 27, 28, // Value
        )
    }
}
