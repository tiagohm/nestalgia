package javafx.scene.image

class RenderedImage(
    private val buffer: IntArray,
    private val width: Int,
    private val height: Int,
) : Image(width, height) {

    fun render() {
        writablePlatformImage.setPixels(0, 0, width, height, PIXEL_FORMAT, buffer, 0, width)
    }

    companion object {

        @JvmStatic private val PIXEL_FORMAT = PixelFormat.getIntArgbPreInstance()
    }
}
