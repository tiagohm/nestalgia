package br.tiagohm.nestalgia.core

object CheatDatabase {

    private val entries = ArrayList<CheatInfo>(16384)

    fun getByGame(crc: Long): List<CheatInfo> {
        return entries.filter { it.crc == crc }
    }

    fun load(data: List<String>) {
        for (line in data) {
            if (line.isEmpty() || line.startsWith("#")) continue
            CheatInfo.parse(line)?.let { entries.add(it) }
        }

        System.err.println("${entries.size} cheats loaded in the database!!!")
    }

    const val CHEAT_DB_FILENAME = "CheatDB.csv"
}
