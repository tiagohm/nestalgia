package br.tiagohm.nestalgia.core

import org.slf4j.LoggerFactory
import java.io.InputStream
import java.nio.file.Path
import java.util.stream.Stream
import kotlin.io.path.inputStream

object GameDatabase {

    const val FILENAME = "NesDB.csv"

    private val ENTRIES = HashMap<Long, GameInfo>(8192)
    private val LOG = LoggerFactory.getLogger(GameDatabase::class.java)

    operator fun get(crc: Long) = ENTRIES[crc]

    val size
        get() = ENTRIES.size

    fun load(data: Stream<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            val game = GameInfo.parse(line)
            ENTRIES[game.crc] = game
        }

        LOG.info("{} games loaded in the database", ENTRIES.size)
    }

    fun load(stream: InputStream) {
        load(stream.bufferedReader().lines())
    }

    fun load(path: Path) {
        path.inputStream().use(::load)
    }
}
