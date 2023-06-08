package br.tiagohm.nestalgia.core

class DecoderVideoFilter(console: Console) : VideoFilter {

    private val palette = console.settings.palette
    private val output = IntArray(Ppu.PIXEL_COUNT)

    private fun decodePpuBuffer(input: IntArray) {
        repeat(output.size) {
            output[it] = palette[input[it]]
        }
    }

    override fun sendFrame(input: IntArray): IntArray {
        decodePpuBuffer(input)
        return output
    }

    override fun takeScreenshot(): IntArray {
        return output
    }
}
