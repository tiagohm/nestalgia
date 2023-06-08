import br.tiagohm.nestalgia.core.Ppu
import br.tiagohm.nestalgia.core.Region
import br.tiagohm.nestalgia.core.StandardControllerButton
import br.tiagohm.nestalgia.core.md5
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.scopes.StringSpecScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.*
import javax.imageio.ImageIO

abstract class NesTesterSpec : StringSpec() {

    private val tester = NesTester()

    protected suspend fun StringSpecScope.test(
        region: Region = Region.NTSC,
        romPath: String = testCase.name.testName, action: (suspend NesTester.() -> Unit)) {
        tester.region = region
        tester.load(Path.of("src/test/resources/roms/$romPath.nes"))

        try {
            tester.action()
        } finally {
            takeScreenshotAndSaveIt()
            tester.stop()
        }
    }

    protected suspend fun StringSpecScope.testWithResetAtStartup(
        resetCount: Int = 1,
        resetDelay: Long = 1500L,
        region: Region = Region.NTSC,
        romPath: String = testCase.name.testName, action: (suspend NesTester.() -> Unit)) {
        test(region, romPath) {
            repeat(resetCount) {
                delay(resetDelay)
                tester.softReset()
            }

            action()
        }
    }

    protected suspend fun StringSpecScope.testWithButtonPressAtStartup(
        button: StandardControllerButton,
        initialDelay: Long = 1000L,
        region: Region = Region.NTSC,
        romPath: String = testCase.name.testName, action: (suspend NesTester.() -> Unit)) {
        test(region, romPath) {
            delay(initialDelay)
            pressButton(button)
            action()
        }
    }

    protected fun StringSpecScope.takeScreenshotAndSaveIt(suffix: String = "") {
        val testName = testCase.name.testName.replace("/", "_")
        val name = "$testName$suffix"
        val screenshot = tester.takeScreenshot()
        val image = BufferedImage(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB)
        screenshot.copyInto((image.raster.dataBuffer as DataBufferInt).data)
        ImageIO.write(image, "PNG", File("src/test/resources/screenshots/$name.png"))
        println("$name: ${screenshot.md5()}")
    }

    protected suspend fun waitForFrame(frameHash: String,
                                       timeoutDuration: Long = 60, timeoutUnit: TimeUnit = SECONDS) {
        withTimeout(timeoutUnit.toMillis(timeoutDuration)) {
            while (frameHash !in tester.frameHashes) delay(100)
        }
    }
}
