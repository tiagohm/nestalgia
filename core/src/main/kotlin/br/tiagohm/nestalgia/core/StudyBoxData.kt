package br.tiagohm.nestalgia.core

data class StudyBoxData(
    @JvmField val fileName: String = "",
    @JvmField val audioFile: ByteArray = ByteArray(0),
    @JvmField val pages: Array<PageInfo> = emptyArray(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as StudyBoxData

        if (fileName != other.fileName) return false
        if (!audioFile.contentEquals(other.audioFile)) return false
        return pages.contentEquals(other.pages)
    }

    override fun hashCode(): Int {
        var result = fileName.hashCode()
        result = 31 * result + audioFile.contentHashCode()
        result = 31 * result + pages.contentHashCode()
        return result
    }

    companion object {

        @JvmStatic val EMPTY = StudyBoxData()
    }
}
