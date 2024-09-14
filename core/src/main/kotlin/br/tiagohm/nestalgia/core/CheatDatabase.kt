package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.inputStream

object CheatDatabase {

    const val FILENAME = "CheatDB.csv"

    private val ENTRIES = ArrayList<CheatInfo>(16384)
    private val LOG = LoggerFactory.getLogger(CheatDatabase::class.java)

    operator fun get(crc: Long): List<CheatInfo> {
        return ENTRIES.filter { it.crc == crc }
    }

    fun load(data: Stream<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            CheatInfo.parse(line)?.let { ENTRIES.add(it) }
        }

        LOG.info("{} cheats loaded in the database", ENTRIES.size)
    }

    fun load(stream: InputStream) {
        load(stream.bufferedReader().lines())
    }

    fun load(path: Path) {
        path.inputStream().use(::load)
    }
}
