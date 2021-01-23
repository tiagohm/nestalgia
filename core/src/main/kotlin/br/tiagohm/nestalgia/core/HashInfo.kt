package br.tiagohm.nestalgia.core

data class HashInfo(
    // PRG
    val prgCrc32: Long = 0L,
    val prgMd5: String = "",
    val prgSha1: String = "",
    val prgSha256: String = "",
    // CHR
    val chrCrc32: Long = 0L,
    val chrMd5: String = "",
    val chrSha1: String = "",
    val chrSha256: String = "",
    // ROM (PRG + CHR)
    val crc32: Long = 0L,
    val md5: String = "",
    val sha1: String = "",
    val sha256: String = "",
) {

    companion object {
        val EMPTY = HashInfo()
    }
}
