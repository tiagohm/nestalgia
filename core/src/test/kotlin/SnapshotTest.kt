import br.tiagohm.nestalgia.core.CpuState
import br.tiagohm.nestalgia.core.Region
import br.tiagohm.nestalgia.core.Snapshot
import br.tiagohm.nestalgia.core.Snapshotable
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.longs.shouldBeExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.io.Serializable

class SnapshotTest : StringSpec() {

    init {
        "write and read boolean" {
            snapshot("a" to true) {
                readBoolean("a").shouldBeTrue()
                readBoolean("b").shouldBeFalse()
            }
        }
        "write and read int" {
            snapshot("a" to 22) {
                readInt("a") shouldBeExactly 22
                readInt("b") shouldBeExactly 0
            }
        }
        "write and read long" {
            snapshot("a" to 22L) {
                readLong("a") shouldBeExactly 22L
                readLong("b") shouldBeExactly 0L
            }
        }
        "write and read snapshotable" {
            val state0 = CpuState(1, 2, 3, 4, 5, 6, 7, true)

            snapshot("a" to state0) {
                val state1 = CpuState()
                state1 shouldNotBe state0
                readSnapshotable("a", state1).shouldBeTrue()
                state1 shouldBe state0
                readSnapshotable("b", state1).shouldBeFalse()
            }
        }
        "write and read snapshot" {
            val snapshot = Snapshot()
            snapshot.write("c", "1")

            snapshot("a" to snapshot) {
                readSnapshot("a").shouldNotBeNull() shouldBe snapshot
                readSnapshot("b").shouldBeNull()
            }
        }
        "write and read enum" {
            snapshot("a" to Region.DENDY) {
                readEnum("a", Region.NTSC) shouldBe Region.DENDY
                readEnum("b", Region.NTSC) shouldBe Region.NTSC
            }
        }
        "write and read short array" {
            snapshot("a" to shortArrayOf(10, 20)) {
                readShortArray("a") shouldBe shortArrayOf(10, 20)
                readShortArray("b").shouldBeNull()
            }
        }
        "write and read int array" {
            snapshot("a" to intArrayOf(10, 20)) {
                readIntArray("a") shouldBe intArrayOf(10, 20)
                readIntArray("b").shouldBeNull()
            }
        }
        "write and read enum array" {
            snapshot("a" to Region.values()) {
                readArray<Region>("a") shouldBe Region.values()
                readArray<Region>("b").shouldBeNull()
            }
        }
        "write and read string" {
            snapshot("a" to "b") {
                readString("a") shouldBe "b"
                readString("b") shouldBe ""
            }
        }
    }

    companion object {

        @Suppress("UNCHECKED_CAST")
        private inline fun snapshot(
            vararg data: Pair<String, Any>,
            action: Snapshot.() -> Unit,
        ) {
            val snapshot = Snapshot()

            for ((key, value) in data) {
                when (value) {
                    is Boolean -> snapshot.write(key, value)
                    is Int -> snapshot.write(key, value)
                    is Long -> snapshot.write(key, value)
                    is Snapshotable -> snapshot.write(key, value)
                    is Snapshot -> snapshot.write(key, value)
                    is Enum<*> -> snapshot.write(key, value)
                    is ShortArray -> snapshot.write(key, value)
                    is IntArray -> snapshot.write(key, value)
                    is Array<*> -> snapshot.write(key, value as Array<out Serializable>)
                    is String -> snapshot.write(key, value)
                }
            }

            snapshot.action()

            with(Snapshot.from(snapshot.bytes())) {
                action()
            }
        }
    }
}
