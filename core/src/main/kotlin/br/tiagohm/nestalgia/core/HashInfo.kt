package br.tiagohm.nestalgia.core

data class HashInfo(
    val romCrc32: Long = 0L,
    val prgCrc32: Long = 0L,
    val chrCrc32: Long = 0L,
    val prgChrCrc32: Long = 0L,
    val sha1: String = "",
    val md5: String = "",
) {

    companion object {
        val EMPTY = HashInfo()
    }
}
