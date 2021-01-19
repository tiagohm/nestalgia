package br.tiagohm.nestalgia.core

@ExperimentalUnsignedTypes
data class RomData(
    val info: RomInfo,
    val chrRamSize: Int = -1,
    val saveChrRamSize: Int = -1,
    val saveRamSize: Int = -1,
    val workRamSize: Int = -1,
    val prgRom: UByteArray,
    val chrRom: UByteArray,
    val treinerData: UByteArray,
    val fdsDiskData: List<UByteArray>?,
    val fdsDiskHeaders: List<UByteArray>?,
    val studyBox: StudyBoxData?,
    val bytes: ByteArray,
    val biosMissing: Boolean,
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
        if (fdsDiskData != other.fdsDiskData) return false
        if (fdsDiskHeaders != other.fdsDiskHeaders) return false
        if (studyBox != other.studyBox) return false
        if (!bytes.contentEquals(other.bytes)) return false
        if (biosMissing != other.biosMissing) return false

        return true
    }

    override fun hashCode(): Int {
        var result = info.hashCode()
        result = 31 * result + chrRamSize
        result = 31 * result + saveChrRamSize
        result = 31 * result + saveRamSize
        result = 31 * result + workRamSize
        result = 31 * result + prgRom.hashCode()
        result = 31 * result + chrRom.hashCode()
        result = 31 * result + treinerData.hashCode()
        result = 31 * result + (fdsDiskData?.hashCode() ?: 0)
        result = 31 * result + (fdsDiskHeaders?.hashCode() ?: 0)
        result = 31 * result + (studyBox?.hashCode() ?: 0)
        result = 31 * result + bytes.hashCode()
        result = 31 * result + biosMissing.hashCode()
        return result
    }
}