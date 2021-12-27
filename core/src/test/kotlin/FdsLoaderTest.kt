import br.tiagohm.nestalgia.core.FdsLoader
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.File

class FdsLoaderTest {

    @Test
    fun loadAndRebuildOneSide() {
        val rawData0 = File(
            FdsLoaderTest::class.java.classLoader.getResources(("1.fds")).nextElement().file
        ).readBytes().toUByteArray()

        val diskSides = ArrayList<UByteArray>()
        val diskHeaders = ArrayList<UByteArray>()

        FdsLoader.loadDiskData(rawData0, diskSides, diskHeaders)
        assertEquals(1, diskSides.size)

        val rawData1 = FdsLoader.rebuildFdsFile(diskSides, false)

        assertEquals(rawData0.size, rawData1.size)
        assertArrayEquals(rawData0.toByteArray(), rawData1.toByteArray())
    }

    @Test
    fun loadAndRebuildTwoSides() {
        val rawData0 = File(
            FdsLoaderTest::class.java.classLoader.getResources(("2.fds")).nextElement().file
        ).readBytes().toUByteArray()

        val diskSides = ArrayList<UByteArray>()
        val diskHeaders = ArrayList<UByteArray>()

        FdsLoader.loadDiskData(rawData0, diskSides, diskHeaders)
        assertEquals(2, diskSides.size)

        val rawData1 = FdsLoader.rebuildFdsFile(diskSides, false)

        assertEquals(rawData0.size, rawData1.size)
        assertArrayEquals(rawData0.toByteArray(), rawData1.toByteArray())
    }
}
