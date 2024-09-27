import br.tiagohm.nestalgia.core.CompressedRomLoader
import br.tiagohm.nestalgia.core.RomData
import br.tiagohm.nestalgia.core.RomFormat
import br.tiagohm.nestalgia.core.hex
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

// find . -type f -name "*.nes" -execdir 7z a '{}.7z' '{}' \;
// find . -name "*.nes.7z" -type f | while read file; do mv "$file" "${file//.nes/}"; done
// cat nes_urls.txt | shuf | xargs -n1 -P4 wget --continue

class NesRomDatabase : Runnable {

    @JacksonXmlRootElement(localName = "nes20db")
    class NesDatabase(
        @JvmField @field:JacksonXmlProperty val date: String = "",
        @JvmField @field:JacksonXmlElementWrapper(useWrapping = false) @field:JsonProperty("game") val games: List<Game> = ArrayList(11000),
    )

    data class Game(
        @JvmField @field:JacksonXmlProperty val name: String = "",
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("prgrom") val prgRom: Rom? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("chrrom") val chrRom: Rom? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("trainer") val trainer: Rom? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("miscrom") val miscRom: Rom? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("prgram") val prgRam: Ram? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("chrram") val chrRam: Ram? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("prgnvram") val prgNVRam: Ram? = null,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("chrnvram") val chrNVRam: Ram? = null,
        @JvmField @field:JacksonXmlProperty val rom: Rom? = null,
        @JvmField @field:JacksonXmlProperty val pcb: Pcb? = null,
        @JvmField @field:JacksonXmlProperty val console: Console? = null,
        @JvmField @field:JacksonXmlProperty val expansion: Expansion? = null,
        @JvmField @field:JacksonXmlProperty val vs: Vs? = null,
    )

    data class Ram(
        @JvmField @field:JacksonXmlProperty val size: Int = 0,
    )

    data class Rom(
        @JvmField @field:JacksonXmlProperty val size: Int = 0,
        @JvmField @field:JacksonXmlProperty val crc32: String = "",
        @JvmField @field:JacksonXmlProperty val sha1: String = "",
        @JvmField @field:JacksonXmlProperty val sum16: String = "",
        @JvmField @field:JacksonXmlProperty val number: Int = 0,
    )

    data class Pcb(
        @JvmField @field:JacksonXmlProperty val mapper: Int = 0,
        @JvmField @field:JacksonXmlProperty @field:JsonProperty("submapper") val subMapper: Int = 0,
        @JvmField @field:JacksonXmlProperty val mirroring: String = "",
        @JvmField @field:JacksonXmlProperty val battery: Int = 0,
    )

    data class Console(
        @JvmField @field:JacksonXmlProperty val type: Int = 0,
        @JvmField @field:JacksonXmlProperty val region: Int = 0,
    )

    data class Expansion(
        @JvmField @field:JacksonXmlProperty val type: Int = 0,
    )

    data class Vs(
        @JvmField @field:JacksonXmlProperty val hardware: Int = 0,
        @JvmField @field:JacksonXmlProperty val ppu: Int = 0,
    )

    object Nes : Table("nes") {
        val id: Column<String> = text("id")
        val name: Column<String> = text("name").index()
        val type: Column<String> = text("type").index()
        val mapper: Column<Int> = integer("mapper").index()
        val subMapper: Column<Int> = integer("subMapper").index()
        val board: Column<String?> = text("board").nullable().index()
        val matched: Column<Boolean> = bool("matched")

        override val primaryKey = PrimaryKey(id, name = "pk_nes_id")
    }

    private val inputDir = Path(requireNotNull(System.getenv("INPUT_DIR")))
    private val outputDir = Path(requireNotNull(System.getenv("OUTPUT_DIR")))

    init {
        require(inputDir.exists() && inputDir.isDirectory())
        require(outputDir.exists() && outputDir.isDirectory())

        Database.connect("jdbc:sqlite:$outputDir/nes.db", driver = "org.sqlite.JDBC")
    }

    private val nesDir = Path("$outputDir", "nes").createDirectories()
    private val binDir = Path("$outputDir", "bin").createDirectories()

    private val gameDatabase = HashMap<String, Game>(12000)

    private fun Path.findRoms(glob: String = "*", block: (Path) -> Boolean) {
        for (path in listDirectoryEntries(glob)) {
            if (path.isDirectory()) {
                path.findRoms(glob, block)
            } else if (!block(path)) {
                break
            }
        }
    }

    private fun loadNesDatabase() {
        val db = Path("core/nes20db.xml").inputStream().use { XML.readValue(it, NesDatabase::class.java) }

        LOG.info("NES Database loaded. date={}, size={}", db.date, db.games.size)

        for (game in db.games) {
            gameDatabase[game.rom!!.sha1.lowercase()] = game
        }
    }

    override fun run() {
        transaction {
            SchemaUtils.create(Nes)
        }

        loadNesDatabase()

        val tasks = LinkedList<Future<*>>()

        LOG.info("starting. path={}", inputDir)

        inputDir.findRoms { path ->
            val ext = path.extension

            if (ext == "7z" || ext == "nes" || ext == "unf") {
                try {
                    val rom = CompressedRomLoader.load(path.readBytes(), path.nameWithoutExtension)

                    if (rom.info.format == RomFormat.INES || rom.info.format == RomFormat.UNIF) {
                        val processor = RomProcessor(rom, path)
                        tasks.add(EXECUTOR.submit(processor))
                        return@findRoms true
                    }
                } catch (e: Throwable) {
                    LOG.error("failed to load ROM. name={}, message={}", path.name, e.message)
                    return@findRoms true
                }
            }

            val outputPath = Path("$binDir", path.name)
            path.copyToWithDuplicate(outputPath)

            return@findRoms true
        }

        tasks.forEach { it.get() }

        LOG.info("finished! count={}", tasks.size)
    }

    private inner class RomProcessor(
        private val rom: RomData,
        private val path: Path,
    ) : Runnable {

        override fun run() {
            try {
                val sha1 = rom.info.hash.sha1
                val game = gameDatabase[sha1]
                val format = rom.info.format

                val outputPath = if (game?.pcb != null) {
                    val name = Path(game.name.replace('\\', '/')).nameWithoutExtension
                    val mapper = "%03d".format(game.pcb.mapper)
                    val subMapper = game.pcb.subMapper.let { if (it > 0) ".$it" else "" }
                    val board = rom.info.unifBoard

                    when (format) {
                        RomFormat.INES -> Path("$nesDir", "$mapper$subMapper.$name.${path.extension}")
                        RomFormat.UNIF -> Path("$nesDir", "$mapper.$board.$name.${path.extension}")
                        else -> return
                    }
                } else {
                    val mapper = "%03d".format(rom.info.mapperId)
                    val subMapper = rom.info.subMapperId.let { if (it > 0) ".$it" else "" }
                    val board = rom.info.unifBoard

                    when (format) {
                        RomFormat.INES -> Path("$nesDir", "$mapper$subMapper.${path.name}")
                        RomFormat.UNIF -> Path("$nesDir", "$mapper.$board.${path.name}")
                        else -> return
                    }
                }

                synchronized(EXECUTOR) {
                    transaction {
                        if (Nes.select(Nes.id).where { Nes.id eq sha1 }.count() == 0L) {
                            val newPath = path.copyToWithDuplicate(outputPath)

                            if (newPath != null) {
                                LOG.info("added. path={}", newPath.nameWithoutExtension)

                                Nes.insert {
                                    it[id] = sha1
                                    it[name] = newPath.nameWithoutExtension
                                    it[type] = rom.info.format.name
                                    it[mapper] = game?.pcb?.mapper ?: rom.info.mapperId
                                    it[subMapper] = game?.pcb?.subMapper ?: rom.info.subMapperId
                                    it[board] = rom.info.unifBoard.ifBlank { null }
                                    it[matched] = game != null
                                }
                            }
                        } else {
                            LOG.warn("already exists. path={}", path.nameWithoutExtension)
                        }
                    }
                }
            } catch (e: Throwable) {
                LOG.error("error", e)
            }
        }
    }

    companion object {

        @JvmStatic
        fun main(args: Array<String>) {
            val database = NesRomDatabase()
            EXECUTOR.submit(database)

            EXECUTOR.awaitTermination(1, TimeUnit.HOURS)
        }

        private val LOG = LoggerFactory.getLogger(NesRomDatabase::class.java)
        private val EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())

        private val XML = XmlMapper().also {
            it.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
        }

        private fun Path.sha1(): String {
            val digest = MessageDigest.getInstance("SHA-1")
            val buffer = ByteArray(1024)

            inputStream().use {
                while (true) {
                    val n = it.read(buffer)

                    if (n > 0) {
                        digest.update(buffer, 0, n)
                    } else {
                        break
                    }
                }
            }

            return digest.digest().hex()
        }

        private fun Path.copyToWithDuplicate(output: Path): Path? {
            var newPath = output
            var index = 1

            while (true) {
                if (!newPath.exists()) {
                    copyTo(newPath, true)
                    return newPath
                } else {
                    val sha1 = sha1()

                    if (sha1 != newPath.sha1()) {
                        newPath = Path("${output.parent}", "${output.nameWithoutExtension}.${index++}.${output.extension}")
                    } else {
                        break
                    }
                }
            }

            return null
        }
    }
}
