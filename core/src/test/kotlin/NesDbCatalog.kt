import br.tiagohm.nestalgia.core.GameDatabase
import br.tiagohm.nestalgia.core.RomLoader
import br.tiagohm.nestalgia.core.hex
import br.tiagohm.nestalgia.core.toIntArray
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import org.slf4j.LoggerFactory
import java.nio.file.Path
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Consumer
import kotlin.io.path.*

data object NesDbCatalog : HashMap<String, Pair<NesDbCatalog.Game, Path>>(11000), Consumer<Path> {

    private fun readResolve(): Any = NesDbCatalog

    private val LOG = LoggerFactory.getLogger(NesDbCatalog::class.java)
    private val HOME_PATH = Path(System.getProperty("user.home"))
    private val OUTPUT_PATH = Path("$HOME_PATH/Nintendinho")
    private val UNKNOWN_PATH = Path("$OUTPUT_PATH", "Unknown")
    private val DUPLICATE_PATH = Path("$OUTPUT_PATH", "Duplicate")
    private val ERROR_PATH = Path("$OUTPUT_PATH", "Error")
    private val OK_PATH = Path("$OUTPUT_PATH", "Ok")
    private val OTHER_PATH = Path("$OUTPUT_PATH", "Other")

    private val EXECUTOR = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
    private val COUNTER = AtomicInteger()

    private val XML = XmlMapper().also {
        it.disable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
    }

    @JvmStatic
    fun main(args: Array<String>) {
        UNKNOWN_PATH.createDirectory()
        DUPLICATE_PATH.createDirectory()
        ERROR_PATH.createDirectory()
        OK_PATH.createDirectory()
        OTHER_PATH.createDirectory()

        makeCatalog()
        EXECUTOR.shutdown()
        EXECUTOR.awaitTermination(1, TimeUnit.HOURS)

        findDuplicates()

        LOG.info("count: {}", COUNTER.get())
    }

    private fun makeCatalog() {
        val nintendinho = Path("")
        loadNesDatabase()
        GameDatabase.load(Path("core/src/main/resources/NesDB.csv"))
        nintendinho.navigate(NesDbCatalog)
    }

    private fun findDuplicates() {
        val hash = HashMap<String, MutableList<Path>>(25000)

        for (path in OK_PATH.listDirectoryEntries()) {
            if (path.isRegularFile()) {
                val sha1 = path.sha1()
                hash.getOrPut(sha1) { ArrayList(4) }.add(path)
            }
        }

        for ((_, value) in hash) {
            if (value.size > 1) {
                value.sortBy { it.name.length }

                for (i in 1 until value.size) {
                    value[i].moveTo(Path("$DUPLICATE_PATH", value[i].name))
                }
            }
        }
    }

    private val buffer = ByteArray(1024)

    private fun Path.sha1(): String {
        val digest = MessageDigest.getInstance("SHA-1")

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

    private fun loadNesDatabase() {
        val db = Path("core/nes20db.xml").inputStream().use { XML.readValue(it, Database::class.java) }
        LOG.info("NES Database loaded. date={}, size={}", db.date, db.games.size)

        for (game in db.games) {
            val name = game.name.lastIndexOf('\\').let { game.name.substring(it + 1) }
            val mapper = "%03d".format(game.pcb!!.mapper)
            this[game.rom!!.sha1.lowercase()] = game to Path("$OK_PATH", "$mapper.$name")
        }
    }

    override fun accept(path: Path) {
        try {
            val rom = RomLoader.load(path.readBytes().toIntArray(), path.name)
            val value = this[rom.info.hash.sha1]

            if (value != null) {
                path.copyToWithDuplicate(value.second)
            } else {
                path.copyToWithDuplicate(Path("$UNKNOWN_PATH", path.name))
            }
        } catch (e: Throwable) {
            LOG.error("failed to load ROM. path={}, message={}", path, e.message)
            path.copyToWithDuplicate(Path("$ERROR_PATH", path.name))
        }
    }

    private fun Path.copyToWithDuplicate(output: Path) {
        var newPath = output
        var index = 1

        while (true) {
            if (!newPath.exists()) {
                copyTo(newPath, true)
                break
            } else {
                newPath = Path("${output.parent}", "${output.nameWithoutExtension}.${index++}.${output.extension}")
            }
        }
    }

    private fun Path.navigate(action: Consumer<Path>) {
        for (path in listDirectoryEntries()) {
            if (path.isDirectory()) {
                path.navigate(action)
            } else {
                COUNTER.incrementAndGet()

                EXECUTOR.submit {
                    val extension = path.extension

                    if (extension == "nes" || extension == "unf") {
                        action.accept(path)
                    } else {
                        path.copyToWithDuplicate(Path("$OTHER_PATH", path.name))
                    }
                }
            }
        }
    }

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
}
