import br.tiagohm.nestalgia.core.Region.PAL
import kotlinx.coroutines.delay

class ApuTest : NesTesterSpec() {

    init {
        "apu_reset/4015_cleared" {
            test {
                delay(1500L)
                softReset()
                waitForFrame("08d296b72730dbc4181d380652e182ea")
            }
        }
        "apu_reset/4017_timing" {
            test {
                delay(1500L)
                softReset()
                waitForFrame("5624e9cf1099eefafdd86c0991d019d1")
            }
        }
        "apu_reset/4017_written" {
            test {
                delay(1500L)
                softReset()
                delay(1500L)
                softReset()
                waitForFrame("aa8cd2924d5f202bb450dc73ca9de803")
            }
        }
        "apu_reset/irq_flag_cleared" {
            test {
                delay(1500L)
                softReset()
                waitForFrame("77d5714fecc758309ae2b79a8e8fef8c")
            }
        }
        "apu_reset/len_ctrs_enabled" {
            test {
                delay(1500L)
                softReset()
                waitForFrame("0fa8870e0811f5ac472017faa7504ab4")
            }
        }
        "apu_reset/works_immediately" {
            test {
                delay(1500L)
                softReset()
                waitForFrame("896c4400309eb62a524cdc0d0de1e6cb")
            }
        }
        "blargg_apu/len_ctr" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/len_table" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/irq_flag" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/clock_jitter" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/len_timing_mode0" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/len_timing_mode1" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/irq_flag_timing" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/irq_timing" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/reset_timing" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/len_halt_timing" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_apu/len_reload_timing" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "apu_test" {
            test {
                waitForFrame("39bce79f8eda157b339c43f9bbf1f93a")
            }
        }
        "blargg_apu/pal/len_ctr" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("7fbbbb516e2713fe3daa4383a0ad2a2e")
            }
        }
        "blargg_apu/pal/len_table" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("732fbb116fcf1df19c26fb3f749882e4")
            }
        }
        "blargg_apu/pal/irq_flag" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("5aaf0bf8a01eb5148c8b537c6a0c9689")
            }
        }
        "blargg_apu/pal/clock_jitter" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("98860cef89ad3c3b0b3a2dde317bbebc")
            }
        }
        "blargg_apu/pal/len_timing_mode0" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("985563df5316a3373bd12d1768bf7603")
            }
        }
        "blargg_apu/pal/len_timing_mode1" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("2dfd41ff2cd4bf9c40cff1b4399f2bc0")
            }
        }
        "blargg_apu/pal/irq_flag_timing" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("2686708fa302432746845568b8fd0d20")
            }
        }
        "blargg_apu/pal/irq_timing" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("88bd69aa88ad89d06ac0abf8e941212c")
            }
        }
        "blargg_apu/pal/len_halt_timing" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("0fa1a36667e5b5dd940780d9137fffcc")
            }
        }
        "blargg_apu/pal/len_reload_timing" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("48579aa73099dc2c8056461ce0141b5c")
            }
        }
    }
}
