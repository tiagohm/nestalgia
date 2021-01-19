package br.tiagohm.nestalgia.core

data class HashInfo(
    val romCrc32: Long,
    val prgCrc32: Long,
    val chrCrc32: Long,
    val prgChrCrc32: Long,
    val sha1: String,
    val md5: String,
)
