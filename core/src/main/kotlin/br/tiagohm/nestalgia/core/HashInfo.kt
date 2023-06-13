package br.tiagohm.nestalgia.core

data class HashInfo(
    // PRG
    @JvmField val prgCrc32: Long = 0L,
    @JvmField val prgMd5: String = "",
    @JvmField val prgSha1: String = "",
    @JvmField val prgSha256: String = "",
    // CHR
    @JvmField val chrCrc32: Long = 0L,
    @JvmField val chrMd5: String = "",
    @JvmField val chrSha1: String = "",
    @JvmField val chrSha256: String = "",
    // ROM (PRG + CHR)
    @JvmField val crc32: Long = 0L,
    @JvmField val md5: String = "",
    @JvmField val sha1: String = "",
    @JvmField val sha256: String = "",
) {

    companion object {

        @JvmStatic val EMPTY = HashInfo()
    }
}
