package br.tiagohm.nestalgia.core

data class RomData(
    @JvmField val info: RomInfo = RomInfo.EMPTY,
    @JvmField val chrRamSize: Int = -1,
    @JvmField val saveChrRamSize: Int = -1,
    @JvmField val saveRamSize: Int = -1,
    @JvmField val workRamSize: Int = -1,
    @JvmField val prgRom: IntArray = IntArray(0),
    @JvmField val chrRom: IntArray = IntArray(0),
    @JvmField val treinerData: IntArray = IntArray(0),
    @JvmField val studyBox: StudyBoxData = StudyBoxData.EMPTY,
    @JvmField val rawData: IntArray = IntArray(0),
    @JvmField val biosMissing: Boolean = false,
    @JvmField val fdsBios: IntArray = IntArray(0),
) {

    companion object {

        val EMPTY = RomData()
    }
}
