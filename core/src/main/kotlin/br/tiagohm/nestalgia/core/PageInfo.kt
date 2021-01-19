package br.tiagohm.nestalgia.core

data class PageInfo(
    val leadInOffset: Int,
    val audioOffset: Int,
    val data: ByteArray,
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PageInfo

        if (leadInOffset != other.leadInOffset) return false
        if (audioOffset != other.audioOffset) return false
        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = leadInOffset
        result = 31 * result + audioOffset
        result = 31 * result + data.contentHashCode()
        return result
    }
}
