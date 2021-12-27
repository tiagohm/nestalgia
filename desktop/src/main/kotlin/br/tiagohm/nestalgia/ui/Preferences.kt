package br.tiagohm.nestalgia.ui

import br.tiagohm.nestalgia.core.EmulationSettings
import br.tiagohm.nestalgia.core.Snapshot
import br.tiagohm.nestalgia.core.Snapshotable
import java.io.File

class Preferences(private val file: File) : Snapshotable {

    val settings = EmulationSettings()
    val recentlyOpened = Array(10) { "" }
    var loadRomDir = ""

    init {
        try {
            restoreState(Snapshot(file.readBytes()))
        } catch (e: Exception) {
            System.err.println(e.message)
        }
    }

    override fun saveState(s: Snapshot) {
        s.write("settings", settings)
        for (i in recentlyOpened.indices) s.writeUtf8("recentlyOpened$i", recentlyOpened[i])
        s.writeUtf8("loadRomDir", loadRomDir)
    }

    override fun restoreState(s: Snapshot) {
        s.load()

        s.readSnapshot("settings")?.let { settings.restoreState(it) }
        for (i in recentlyOpened.indices) s.readUtf8("recentlyOpened$i")?.let { recentlyOpened[i] = it }
        loadRomDir = s.readUtf8("loadRomDir") ?: ""
    }

    fun save() {
        val snapshot = Snapshot()
        saveState(snapshot)
        file.writeBytes(snapshot.bytes)
    }
}