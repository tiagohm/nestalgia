package br.tiagohm.nestalgia.core

data class RomData(
    val info: RomInfo = RomInfo.EMPTY,
    val chrRamSize: Int = -1,
    val saveChrRamSize: Int = -1,
    val saveRamSize: Int = -1,
    val workRamSize: Int = -1,
    val prgRom: IntArray = IntArray(0),
    val chrRom: IntArray = IntArray(0),
    val treinerData: IntArray = IntArray(0),
    val studyBox: StudyBoxData = StudyBoxData.EMPTY,
    val rawData: IntArray = IntArray(0),
    val biosMissing: Boolean = false,
    val fdsBios: IntArray = IntArray(0),
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RomData

        if (info != other.info) return false
        if (chrRamSize != other.chrRamSize) return false
        if (saveChrRamSize != other.saveChrRamSize) return false
        if (saveRamSize != other.saveRamSize) return false
        if (workRamSize != other.workRamSize) return false
        if (!prgRom.contentEquals(other.prgRom)) return false
        if (!chrRom.contentEquals(other.chrRom)) return false
        if (!treinerData.contentEquals(other.treinerData)) return false
        if (studyBox != other.studyBox) return false
        if (!rawData.contentEquals(other.rawData)) return false
        return biosMissing == other.biosMissing
    }

    override fun hashCode(): Int {
        var result = info.hashCode()
        result = 31 * result + chrRamSize
        result = 31 * result + saveChrRamSize
        result = 31 * result + saveRamSize
        result = 31 * result + workRamSize
        result = 31 * result + prgRom.contentHashCode()
        result = 31 * result + chrRom.contentHashCode()
        result = 31 * result + treinerData.contentHashCode()
        result = 31 * result + studyBox.hashCode()
        result = 31 * result + rawData.contentHashCode()
        result = 31 * result + biosMissing.hashCode()
        result = 31 * result + fdsBios.contentHashCode()
        return result
    }

    companion object {

        @JvmStatic val EMPTY = RomData()
    }
}
