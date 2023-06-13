package br.tiagohm.nestalgia.core

data class PageInfo(
    @JvmField val leadInOffset: Int,
    @JvmField val audioOffset: Int,
    @JvmField val data: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageInfo

        if (leadInOffset != other.leadInOffset) return false
        if (audioOffset != other.audioOffset) return false
        return data.contentEquals(other.data)
    }

    override fun hashCode(): Int {
        var result = leadInOffset
        result = 31 * result + audioOffset
        result = 31 * result + data.contentHashCode()
        return result
    }
}
