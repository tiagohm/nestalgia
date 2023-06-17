import br.tiagohm.nestalgia.core.ControllerType.*
import br.tiagohm.nestalgia.core.StandardControllerButton.*

// https://github.com/pinobatch/allpads-nes

class InputTest : NesTesterSpec() {

    init {
        "allpads/nes_controller_1P" {
            test("allpads", false) {
                console.settings.port1.type = NES_CONTROLLER
                NesTester.CONTROLLER_KEYS_1P.copyTo(console.settings.port1.keyMapping)
                console.settings.port2.type = NONE

                waitForFrame("b9aeb3e878183816371f17b2b3ce2d23")
                pressAndRelease(A)
                waitForFrame("8207ba9d2b62a05360ab33598737da26")

                for ((key, hash) in NES_CONTROLLER_1P_STATES) {
                    press(key)
                    waitForFrame(hash)
                    release(key)
                }
            }
        }
        "allpads/nes_controller_2P" {
            test("allpads", false) {
                console.settings.port1.type = NES_CONTROLLER
                NesTester.CONTROLLER_KEYS_1P.copyTo(console.settings.port1.keyMapping)
                console.settings.port2.type = NES_CONTROLLER
                NesTester.CONTROLLER_KEYS_2P.copyTo(console.settings.port2.keyMapping)

                waitForFrame("3e6320695be26d70e9072433f44148e0")
                pressAndRelease(A, 1)
                waitForFrame("bf128e51ffe0801ec1c813c0db517ba9")

                for ((key, hash) in NES_CONTROLLER_2P_STATES) {
                    press(key, 1)
                    waitForFrame(hash)
                    release(key, 1)
                }
            }
        }
        "allpads/four_score" {
            test("allpads", false) {
                console.settings.port1.type = FOUR_SCORE
                console.settings.port2.type = NONE

                console.settings.subPort1[0].type = NES_CONTROLLER
                console.settings.subPort1[1].type = NES_CONTROLLER
                console.settings.subPort1[2].type = NES_CONTROLLER
                console.settings.subPort1[3].type = NES_CONTROLLER

                NesTester.CONTROLLER_KEYS_1P.copyTo(console.settings.subPort1[0].keyMapping)
                NesTester.CONTROLLER_KEYS_2P.copyTo(console.settings.subPort1[1].keyMapping)
                NesTester.CONTROLLER_KEYS_3P.copyTo(console.settings.subPort1[2].keyMapping)
                NesTester.CONTROLLER_KEYS_4P.copyTo(console.settings.subPort1[3].keyMapping)

                waitForFrame("979e10df063d6f55bcb77242509dff46")
                pressAndRelease(A)
                waitForFrame("4dad91db19364460c7f727890f261ba1")

                FOUR_SCORE_PORT_STATES.forEachIndexed { port, state ->
                    for ((key, hash) in state) {
                        press(key, port)
                        waitForFrame(hash)
                        release(key, port)
                    }
                }
            }
        }
        "allpads/zapper_1P" {
            test("allpads", false) {
                console.settings.port1.type = NES_ZAPPER

                waitForFrame("1cdf170fe5a452186ea8a4b46e2839f1")
            }
        }
        "allpads/zapper_2P" {
            test("allpads", false) {
                console.settings.port1.type = NES_ZAPPER
                console.settings.expansionPort.type = FAMICOM_ZAPPER

                waitForFrame("cee60e578bbc9fda40d9b8a9a9405836")
            }
        }
        "allpads/arkanoid_1P" {
            test("allpads", false) {
                console.settings.port1.type = NES_ARKANOID_CONTROLLER
                console.settings.expansionPort.type = NONE

                waitForFrame("b3f6a2958d9dda15b06a73d3415e3d20")
            }
        }
        "allpads/none" {
            test("allpads", false) {
                console.settings.port1.type = NONE
                isAutoPlugController = false

                waitForFrame("c298fd8b8aa644339989af01ecb64294")
            }
        }
    }

    companion object {

        @JvmStatic private val NES_CONTROLLER_1P_STATES = mapOf(
            A to "537b3d6d36c0b6f555e07cf40bd2fc22",
            B to "607f6384c877ab00e7a6ad70193ad028",
            SELECT to "a32285236280acf49f4afb6f3ec85b71",
            START to "6e713ed259afd289b224000ab1500d61",
            UP to "266144f35cfd31dbe73a658c33c365d9",
            DOWN to "3ef57343c621dd79356c5ef10f6d7388",
            LEFT to "19c28f08d4ce4feacea2fc4262a15710",
            RIGHT to "0271fe1978e6b4d3e402bea34aecc240",
        )

        @JvmStatic private val NES_CONTROLLER_2P_STATES = mapOf(
            A to "9154d60b6fb4fc5c0c7b2474c3d47476",
            B to "95e34fc822057b1d8c4735b153d83bbc",
            SELECT to "516fc022ab3819cf41a20e7ff67ca1ac",
            START to "43ab7512d4dbf575389be072e605b52b",
            UP to "a6d88a7914bd8c10729438db4cbdc24d",
            DOWN to "84941f55544b8346c4423663e6552613",
            LEFT to "05b0d59968690c8f2a22710d2dbfe6e9",
            RIGHT to "b4ac3a3e51da3db7355fd94b919902c3",
        )

        @JvmStatic private val FOUR_SCORE_1P_STATES = mapOf(
            A to "ec6b56f5df412e0221226ce17341a297",
            B to "c285c56048902ce36c29fe3dcd9dd366",
            SELECT to "bb65d0ba475a56eae5fe8701632ee873",
            START to "85292a9a1c65d9aeb2c0c15fd48c2f81",
            UP to "698b57851ba1c5809a3f3e18a0d5d5f2",
            DOWN to "0664f50af7256540304f97d0797c0c00",
            LEFT to "ecfe801ddd67673cf310c8516708f91d",
            RIGHT to "efa6c1076aa58df5afccbbea9c49e82d",
        )

        @JvmStatic private val FOUR_SCORE_2P_STATES = mapOf(
            A to "0b819ae1a0915a0cf28cfcd6e9da2dec",
            B to "73eebf1864b855100b9384a642bd1f14",
            SELECT to "6c3b897d4261cdf04abc53cfde838429",
            START to "1a3e0efe022faf82b4e9575acb09001e",
            UP to "84944ea87c7cd2a7fa448dea43062b65",
            DOWN to "350f8d6df940700da6ec3634c49ea2a9",
            LEFT to "4c69ec4ed50dc8d8a570d57944e9f671",
            RIGHT to "20ab5a4a987c3360d3fde35b945ecb11",
        )

        @JvmStatic private val FOUR_SCORE_3P_STATES = mapOf(
            A to "e93154d7febf5dbc0f96da60a5ac8fa7",
            B to "a782582adb13d580c325a1771ac7b289",
            SELECT to "323b2346042b9244e8c9b1ee9fba6e8b",
            START to "df6090b74703c67b37869ccf07102f98",
            UP to "35ea28c0438a67d105811046f5d32988",
            DOWN to "e244e7df3d2efc56ac7709b55c5efe8a",
            LEFT to "f572c90b6e009dcfb6ef15fc4cd8ca54",
            RIGHT to "cd52e2140483a40b79c5ced00f71fe45",
        )

        @JvmStatic private val FOUR_SCORE_4P_STATES = mapOf(
            A to "dc032df0eac4d1583a89c0dc3b3f3d94",
            B to "e1445511bf982e797c873f66b5c41209",
            SELECT to "94ec14ef6b8745832dd66e9440ffce1c",
            START to "0434b636e5cf38ee82adbd97c3b1422e",
            UP to "038073fe11cd6ed3e364993ea89d1ea4",
            DOWN to "faa5c122d49e81a6d3be9947b8411a7f",
            LEFT to "6b5afcb94a4a37061a39b4e832b10c4a",
            RIGHT to "5845f30f1f27787e7047afba2774545f",
        )

        @JvmStatic private val FOUR_SCORE_PORT_STATES = arrayOf(
            FOUR_SCORE_1P_STATES, FOUR_SCORE_2P_STATES,
            FOUR_SCORE_3P_STATES, FOUR_SCORE_4P_STATES,
        )
    }
}
