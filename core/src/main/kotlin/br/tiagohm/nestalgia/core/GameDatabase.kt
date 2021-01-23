package br.tiagohm.nestalgia.core

import java.util.*

@ExperimentalUnsignedTypes
object GameDatabase {

    private val entries = HashMap<Long, GameInfo>(8192)

    fun get(crc: Long) = entries[crc]

    fun load(data: List<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            val game = GameInfo.parse(line)
            entries[game.crc] = game
        }

        System.err.println("${entries.size} games loaded in the database!!!")
    }

    const val NES_DB_FILENAME = "NesDB.csv"
}