package br.tiagohm.nestalgia.core

data class CheatInfo(
    @JvmField val crc: Long,
    @JvmField val name: String,
    @JvmField val type: CheatType,
    @JvmField val gameGenieCode: String?,
    @JvmField val proActionRockyCode: Int?,
    @JvmField val address: Int?,
    @JvmField val value: Int?,
    @JvmField val compareValue: Int,
    @JvmField val isRelativeAddress: Boolean,
    @JvmField val description: String,
) {

    companion object {

        @JvmStatic
        fun parse(line: String): CheatInfo? {
            val parts = line.split(";")
            val hash = parts[0].also { if (it.isBlank()) return null }
            val name = parts[1].also { if (it.isBlank()) return null }
            val genie = parts[2]
            val rocky = parts[3]
            val address = parts[4]
            val value = parts[5]
            val compare = parts[6]
            val isPrgOffset = parts[7] == "1"
            val description = parts[8]
            val type = when {
                genie.isNotBlank() -> CheatType.GAME_GENIE
                rocky.isNotBlank() -> CheatType.PRO_ACTION_ROCKY
                else -> CheatType.CUSTOM
            }

            return CheatInfo(
                hash.toLong(16), name, type,
                genie.ifBlank { null },
                if (rocky.isBlank()) null else rocky.toInt(16),
                if (address.isBlank()) null else address.toInt(16),
                if (value.isBlank()) null else value.toInt(16),
                if (compare.isBlank()) -1 else compare.toInt(16),
                type == CheatType.CUSTOM && !isPrgOffset,
                description,
            )
        }
    }
}
