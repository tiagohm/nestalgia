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
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import kotlin.io.path.*

// find . -type f -name "*.unf" -execdir 7z a '{}.7z' '{}' \;
// find . -name "*.nes.7z" -type f | while read file; do mv "$file" "${file//.nes/}"; done

class NesRomDatabase : Runnable {

    @JacksonXmlRootElement(localName = "nes20db")
    class Database(
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

    private val inputDir = Path(requireNotNull(System.getenv("INPUT_DIR")))
    private val outputDir = Path(requireNotNull(System.getenv("OUTPUT_DIR")))

    init {
        require(inputDir.exists() && inputDir.isDirectory())
        require(outputDir.exists() && outputDir.isDirectory())
    }

    private val errorDir = Path("$outputDir", "error").createDirectories()
    private val nesDir = Path("$outputDir", "nes").createDirectories()
    private val nesOkDir = Path("$nesDir", "ok").createDirectories()
    private val nesUnknownDir = Path("$nesDir", "unknown").createDirectories()
    private val unifDir = Path("$outputDir", "unif").createDirectories()
    private val unifOkDir = Path("$unifDir", "ok").createDirectories()
    private val unifUnknownDir = Path("$unifDir", "unknown").createDirectories()
    private val fdsDir = Path("$outputDir", "fds").createDirectories()
    private val nsfDir = Path("$outputDir", "nsf").createDirectories()
    private val studyBoxDir = Path("$outputDir", "studyBox").createDirectories()
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
        val db = Path("core/nes20db.xml").inputStream().use { XML.readValue(it, Database::class.java) }

        LOG.info("NES Database loaded. date={}, size={}", db.date, db.games.size)

        for (game in db.games) {
            gameDatabase[game.rom!!.sha1.lowercase()] = game
        }
    }

    override fun run() {
        loadNesDatabase()

        val tasks = LinkedList<Future<*>>()

        LOG.info("starting. path={}", inputDir)

        inputDir.findRoms {
            val ext = it.extension

            if (ext == "7z") {
                try {
                    val rom = CompressedRomLoader.load(it.readBytes(), it.nameWithoutExtension)
                    val processor = RomProcessor(rom, it)
                    tasks.add(EXECUTOR.submit(processor))
                } catch (e: Throwable) {
                    LOG.error("failed to load ROM. name={}, message={}", it.name, e.message)

                    val outputPath = Path("$errorDir", it.name)
                    it.copyToWithDuplicate(outputPath)
                }
            } else if (ext == "fds") {
                val outputPath = Path("$fdsDir", it.name)
                it.copyToWithDuplicate(outputPath)
            } else if (ext == "nsf") {
                val outputPath = Path("$nsfDir", it.name)
                it.copyToWithDuplicate(outputPath)
            } else if (ext != "nes" && ext != "unf") {
                val outputPath = Path("$binDir", it.name)
                it.copyToWithDuplicate(outputPath)
            }

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
            val game = gameDatabase[rom.info.hash.sha1]
            val format = rom.info.format
            var duplicate = true

            val outputPath = if (format == RomFormat.FDS) {
                Path("$fdsDir", path.name)
            } else if (format == RomFormat.NSF) {
                Path("$nsfDir", path.name)
            } else if (format == RomFormat.STUDY_BOX) {
                Path("$studyBoxDir", path.name)
            } else if (game?.pcb != null) {
                val name = Path(game.name.replace('\\', '/')).nameWithoutExtension

                duplicate = false

                when (format) {
                    RomFormat.INES -> {
                        val mapper = "%03d".format(game.pcb.mapper)
                        val subMapper = game.pcb.subMapper.let { if (it > 0) ".$it" else "" }
                        Path("$nesOkDir", "$mapper$subMapper.$name.${path.extension}")
                    }
                    RomFormat.UNIF -> {
                        val mapper = "%03d".format(game.pcb.mapper)
                        val board = rom.info.unifBoard
                        Path("$unifOkDir", "$mapper.$board.$name.${path.extension}")
                    }
                    else -> return
                }
            } else {
                val mapper = "%03d".format(rom.info.mapperId)
                val subMapper = rom.info.subMapperId.let { if (it > 0) "-$it" else "" }

                val baseDir = when (format) {
                    RomFormat.INES -> nesUnknownDir
                    RomFormat.UNIF -> unifUnknownDir
                    else -> return
                }

                Path("$baseDir", "$mapper$subMapper.${path.name}")
            }

            if (!path.copyToWithDuplicate(outputPath, duplicate)) {
                LOG.warn("file already exists! name={}", outputPath.nameWithoutExtension)
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

        private fun Path.copyToWithDuplicate(output: Path, duplicate: Boolean = true): Boolean {
            var newPath = output
            var index = 1

            while (true) {
                if (!newPath.exists()) {
                    copyTo(newPath, true)
                    return true
                } else if (duplicate) {
                    val sha1 = sha1()

                    if (sha1 != newPath.sha1()) {
                        newPath = Path("${output.parent}", "${output.nameWithoutExtension}.${index++}.${output.extension}")
                    } else {
                        break
                    }
                } else {
                    break
                }
            }

            return false
        }
    }
}
