package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory
import java.util.stream.Stream

object GameDatabase {

    const val FILENAME = "NesDB.csv"

    private val ENTRIES = HashMap<Long, GameInfo>(8192)
    private val LOG = LoggerFactory.getLogger(GameDatabase::class.java)

    operator fun get(crc: Long) = ENTRIES[crc]

    fun load(data: Stream<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            val game = GameInfo.parse(line)
            ENTRIES[game.crc] = game
        }

        LOG.info("{} games loaded in the database", ENTRIES.size)
    }
}
