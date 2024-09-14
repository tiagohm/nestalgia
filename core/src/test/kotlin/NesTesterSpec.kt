import br.tiagohm.nestalgia.core.Console
import br.tiagohm.nestalgia.core.Ppu
import br.tiagohm.nestalgia.core.md5
import io.kotest.core.spec.style.StringSpec
import io.kotest.core.spec.style.scopes.StringSpecScope
import io.kotest.matchers.booleans.shouldBeTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import java.io.File
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.SECONDS
import javax.imageio.ImageIO

abstract class NesTesterSpec : StringSpec() {

    protected suspend fun StringSpecScope.test(
        romPath: String = testCase.name.testName,
        autoStart: Boolean = true,
        action: (suspend NesTester.() -> Unit),
    ) {
        val path = Path.of("src/test/resources/roms/$romPath.nes")
        val tester = NesTester(path)

        try {
            if (autoStart) {
                tester.start()
            }

            tester.action()
            tester.console.isRunning.shouldBeTrue()
        } finally {
            takeScreenshotAndSaveIt(tester.console)
            tester.stop()
        }
    }

    protected fun StringSpecScope.takeScreenshotAndSaveIt(console: Console, suffix: String = "") {
        val testName = testCase.name.testName.replace("/", "_")
        val name = "$testName$suffix"
        val screenshot = console.takeScreenshot()
        val image = BufferedImage(Ppu.SCREEN_WIDTH, Ppu.SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB)
        screenshot.copyInto((image.raster.dataBuffer as DataBufferInt).data)
        ImageIO.write(image, "PNG", File("src/test/resources/screenshots/$name.png"))
        println("$name: ${screenshot.md5()}")
    }

    protected suspend fun NesTester.waitForFrame(
        frameHash: String,
        startIfNotRunning: Boolean = true,
        duration: Long = 60, unit: TimeUnit = SECONDS,
    ) {
        if (!console.isRunning && startIfNotRunning) {
            start()
        }

        withTimeout(unit.toMillis(duration)) {
            while (frameHash !in frameHashes) delay(500)
        }
    }
}
