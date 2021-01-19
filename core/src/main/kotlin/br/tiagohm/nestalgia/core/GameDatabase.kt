package br.tiagohm.nestalgia.core

import java.util.*

@ExperimentalUnsignedTypes
object GameDatabase {
    val entries = HashMap<Long, GameInfo>(8192)

    fun load(data: List<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            val game = GameInfo.parse(line)
            entries[game.crc] = game
        }

        System.err.println("${entries.size} games loaded in the database!!!")
    }
}