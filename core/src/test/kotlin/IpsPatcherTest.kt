import br.tiagohm.nestalgia.core.IpsPatcher
import br.tiagohm.nestalgia.core.endsWith
import br.tiagohm.nestalgia.core.startsWith
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import kotlin.random.Random.Default.nextInt

class IpsPatcherTest : StringSpec() {

    init {
        "create and patch" {
            val ipsData = IpsPatcher.create(ORIG_DATA, MOD_DATA)
            val output = IpsPatcher.patch(ipsData, ORIG_DATA)

            MOD_DATA.size shouldBeExactly output.size
            MOD_DATA shouldBe output
        }
        "create and patch randomly" {
            repeat(100) {
                val origData = IntArray(nextInt(300, 601)) { nextInt(256) }
                val modData = origData.clone()
                repeat(nextInt(1, 600)) { modData[nextInt(modData.size)] = nextInt(256) }

                val ipsData = IpsPatcher.create(origData, modData)
                val output = IpsPatcher.patch(ipsData, origData)

                modData.size shouldBeExactly output.size
                modData shouldBe output
            }
        }
        "no patch" {
            val ipsData = IpsPatcher.create(ORIG_DATA, ORIG_DATA)
            ipsData.size shouldBeExactly 8
            ipsData.startsWith("PATCH").shouldBeTrue()
            ipsData.endsWith("EOF").shouldBeTrue()

            val output = IpsPatcher.patch(ipsData, ORIG_DATA)
            output.size shouldBeExactly 32
            output shouldBe ORIG_DATA
        }
    }

    companion object {

        @JvmStatic private val ORIG_DATA = intArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7,
            0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7,
        )

        @JvmStatic private val MOD_DATA = intArrayOf(
            0, 1, 2, 3, 4, 5, 6, 7, 0, 1, 4, 3, 4, 5, 6, 7,
            0, 1, 2, 3, 6, 6, 6, 7, 0, 1, 2, 3, 4, 5, 6, 7,
        )
    }
}
