package br.tiagohm.nestalgia.desktop.app

import br.tiagohm.nestalgia.core.EmulationSettings
import br.tiagohm.nestalgia.core.Snapshot
import br.tiagohm.nestalgia.core.Snapshotable
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.outputStream

@Component
class Preferences(
    @Autowired appDir: Path,
    @Autowired @Qualifier("globalSettings") val settings: EmulationSettings,
) : Snapshotable {

    private val path = Path.of("$appDir", "config.nst")

    val recentlyOpened = arrayOfNulls<Path>(10)
    var loadRomDir = ""

    init {
        try {
            if (path.exists()) {
                val snapshot = path.inputStream().use { Snapshot.from(it) }
                restoreState(snapshot)
            }
        } catch (e: Throwable) {
            LOG.error("Unable to read preferences", e)
        }
    }

    final override fun saveState(s: Snapshot) {
        s.write("settings", settings)
        s.write("recentlyOpened", recentlyOpened.map { "$it" }.toTypedArray())
        s.write("loadRomDir", loadRomDir)
    }

    final override fun restoreState(s: Snapshot) {
        s.readSnapshotable("settings", settings)
        s.readArray<String>("recentlyOpened")?.map(Path::of)?.toTypedArray()?.copyInto(recentlyOpened)
        loadRomDir = s.readString("loadRomDir")
    }

    fun save() {
        val snapshot = Snapshot()
        saveState(snapshot)
        path.outputStream().use(snapshot::writeTo)
    }

    companion object {

        private val LOG = LoggerFactory.getLogger(Preferences::class.java)
    }
}
