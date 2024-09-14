package br.tiagohm.nestalgia.core

data class NsfHeader(
    @JvmField val header: String,
    @JvmField val version: Byte,
    @JvmField val totalSongs: Byte,
    @JvmField val startingSong: Byte,
    @JvmField val loadAddress: Short,
    @JvmField val initAddress: Short,
    @JvmField val playAddress: Short,
    @JvmField val songName: String,
    @JvmField val artistName: String,
    @JvmField val copyrightHolder: String,
    @JvmField val playSpeedNtsc: Short,
    @JvmField val bankSetup: ByteArray,
    @JvmField val playSpeedPal: Short,
    @JvmField val flags: Byte,
    @JvmField val soundChips: Byte,
)
