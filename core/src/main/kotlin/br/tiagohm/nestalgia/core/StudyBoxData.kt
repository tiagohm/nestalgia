package br.tiagohm.nestalgia.core

data class StudyBoxData(
    @JvmField val fileName: String = "",
    @JvmField val audioFile: ByteArray = ByteArray(0),
    @JvmField val pages: Array<PageInfo> = emptyArray(),
) {

    companion object {

        val EMPTY = StudyBoxData()
    }
}
