import br.tiagohm.nestalgia.core.IpsPatcher
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class IpsPatcherTest {

    @Test
    fun createPatchAndApplyIt() {
        val ipsData = IpsPatcher.createPatch(ORIG_DATA, MOD_DATA)

        val output = ArrayList<UByte>()
        assertEquals(true, IpsPatcher.patch(ipsData, ORIG_DATA, output))

        for (i in MOD_DATA.indices) assertEquals(MOD_DATA[i], output[i])
    }

    companion object {
        val ORIG_DATA = ubyteArrayOf(
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
        )

        val MOD_DATA = ubyteArrayOf(
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
            0U, 1U, 4U, 3U, 4U, 5U, 6U, 7U,
            0U, 1U, 2U, 3U, 6U, 6U, 6U, 7U,
            0U, 1U, 2U, 3U, 4U, 5U, 6U, 7U,
        )
    }
}