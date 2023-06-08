import br.tiagohm.nestalgia.core.FdsLoader
import br.tiagohm.nestalgia.core.toIntArray
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe

@Ignored
class FdsLoaderTest : StringSpec() {

    init {
        "load and rebuild one side" {
            val rawData0 = Thread.currentThread().contextClassLoader
                .getResourceAsStream("1.fds")!!.readAllBytes().toIntArray()

            val diskSides = ArrayList<IntArray>()
            val diskHeaders = ArrayList<IntArray>()

            FdsLoader.loadDiskData(rawData0, diskSides, diskHeaders)
            diskSides.size shouldBeExactly 1

            val rawData1 = FdsLoader.rebuildFdsFile(diskSides, false)

            rawData0.size shouldBeExactly rawData1.size
            rawData0 shouldBe rawData1
        }
        "load and rebuild two side" {
            val rawData0 = Thread.currentThread().contextClassLoader
                .getResourceAsStream("2.fds")!!.readAllBytes().toIntArray()

            val diskSides = ArrayList<IntArray>()
            val diskHeaders = ArrayList<IntArray>()

            FdsLoader.loadDiskData(rawData0, diskSides, diskHeaders)
            diskSides.size shouldBeExactly 2

            val rawData1 = FdsLoader.rebuildFdsFile(diskSides, false)

            rawData0.size shouldBeExactly rawData1.size
            rawData0 shouldBe rawData1
        }
    }
}
