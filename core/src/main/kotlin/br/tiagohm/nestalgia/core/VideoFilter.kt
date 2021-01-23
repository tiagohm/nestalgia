package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
@ExperimentalUnsignedTypes
abstract class VideoFilter(val console: Console) : Disposable {

    protected val buffer: IntArray = IntArray(Ppu.PIXEL_COUNT)

    override fun dispose() {
    }

    open fun applyFilter(outputBuffer: UShortArray) {
    }

    open fun onBeforeApplyFilter() {
    }

    fun sendFrame(outputBuffer: UShortArray, frameNumber: Int): IntArray {
        onBeforeApplyFilter()
        applyFilter(outputBuffer)
        return buffer
    }

    open fun takeScreenshot() = buffer
}