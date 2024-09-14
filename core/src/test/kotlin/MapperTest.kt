import br.tiagohm.nestalgia.core.EmulationFlag.MMC3_IRQ_ALT_BEHAVIOR

class MapperTest : NesTesterSpec() {

    init {
        "mmc3_irq/clocking" {
            test {
                waitForFrame("969627a48e39152755c8a140067c0ab4")
            }
        }
        "mmc3_irq/details" {
            test {
                waitForFrame("38d056d435463e3ae7c14988a978586d")
            }
        }
        "mmc3_irq/a12_clocking" {
            test {
                waitForFrame("658d49f306d12b13d289885b5d0737df")
            }
        }
        "mmc3_irq/scanline_timing" {
            test {
                waitForFrame("7a3d33dd8df0d0fc3838b8a8cfd5ddeb")
            }
        }
        "mmc3_irq/rev_A" {
            test(autoStart = false) {
                console.settings.flag(MMC3_IRQ_ALT_BEHAVIOR, true)
                waitForFrame("90a137cbc96cfcad72a995218e119545")
            }
        }
        "mmc3_irq/rev_B" {
            test {
                waitForFrame("9725f70cc6d731e02fd8aaa168de935f")
            }
        }
        "vrc6" {
            test {
                waitForFrame("02c92c7b70729af20f97732e4f912a09")
            }
        }
    }
}
