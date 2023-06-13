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
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NsfHeader

        if (header != other.header) return false
        if (version != other.version) return false
        if (totalSongs != other.totalSongs) return false
        if (startingSong != other.startingSong) return false
        if (loadAddress != other.loadAddress) return false
        if (initAddress != other.initAddress) return false
        if (playAddress != other.playAddress) return false
        if (songName != other.songName) return false
        if (artistName != other.artistName) return false
        if (copyrightHolder != other.copyrightHolder) return false
        if (playSpeedNtsc != other.playSpeedNtsc) return false
        if (!bankSetup.contentEquals(other.bankSetup)) return false
        if (playSpeedPal != other.playSpeedPal) return false
        if (flags != other.flags) return false
        return soundChips == other.soundChips
    }

    override fun hashCode(): Int {
        var result = header.hashCode()
        result = 31 * result + version
        result = 31 * result + totalSongs
        result = 31 * result + startingSong
        result = 31 * result + loadAddress
        result = 31 * result + initAddress
        result = 31 * result + playAddress
        result = 31 * result + songName.hashCode()
        result = 31 * result + artistName.hashCode()
        result = 31 * result + copyrightHolder.hashCode()
        result = 31 * result + playSpeedNtsc
        result = 31 * result + bankSetup.contentHashCode()
        result = 31 * result + playSpeedPal
        result = 31 * result + flags
        result = 31 * result + soundChips
        return result
    }
}
