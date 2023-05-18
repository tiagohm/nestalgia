package br.tiagohm.nestalgia.core

data class CheatInfo(
    val crc: Long,
    val name: String,
    val type: CheatType,
    val gameGenieCode: String?,
    val proActionRockyCode: Int?,
    val address: Int?,
    val value: Int?,
    val compareValue: Int,
    val isRelativeAddress: Boolean,
    val description: String,
) {

    override fun toString() = description

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
                if (genie.isBlank()) null else genie,
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
