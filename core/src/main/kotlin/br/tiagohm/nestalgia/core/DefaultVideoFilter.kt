package br.tiagohm.nestalgia.core

@Suppress("NOTHING_TO_INLINE")
class DefaultVideoFilter(console: Console) : VideoFilter(console) {

    private val palette = console.settings.palette

    private inline fun decodePpuBuffer(outputBuffer: UShortArray, output: IntArray) {
        var out = 0

        val bottom = 0
        val top = 0
        val left = 0
        val right = 0

        for (i in top until 240 - bottom) {
            val offset = i * 256

            for (j in left until 256 - right) {
                output[out++] = palette[outputBuffer[offset + j].toInt()].toInt()
            }
        }
    }

    override fun applyFilter(outputBuffer: UShortArray) {
        decodePpuBuffer(outputBuffer, buffer)
    }
}