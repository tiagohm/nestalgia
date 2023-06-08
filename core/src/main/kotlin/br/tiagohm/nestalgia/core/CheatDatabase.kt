package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory
import java.util.stream.Stream

object CheatDatabase {

    const val FILENAME = "CheatDB.csv"

    @JvmStatic private val ENTRIES = ArrayList<CheatInfo>(16384)
    @JvmStatic private val LOG = LoggerFactory.getLogger(CheatDatabase::class.java)

    @JvmStatic
    operator fun get(crc: Long): List<CheatInfo> {
        return ENTRIES.filter { it.crc == crc }
    }

    @JvmStatic
    fun load(data: Stream<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            CheatInfo.parse(line)?.let { ENTRIES.add(it) }
        }

        LOG.info("{} cheats loaded in the database", ENTRIES.size)
    }
}
