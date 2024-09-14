import br.tiagohm.nestalgia.core.StandardController.Button.START
import kotlinx.coroutines.delay

class CpuTest : NesTesterSpec() {

    init {
        "branch_timing/branch_basics" {
            test {
                waitForFrame("ec9818dea120ad98f69a80cf55b2c551")
            }
        }
        "branch_timing/backward_branch" {
            test {
                waitForFrame("a450f7a49229cc4a76cd054e7e65cd39")
            }
        }
        "branch_timing/forward_branch" {
            test {
                waitForFrame("0ceda3d12006b144df984ea1a5d3bb26")
            }
        }
        "cpu_dummy_reads" {
            test {
                waitForFrame("1800909d1611a51a804dbdeb7b73aeca")
            }
        }
        "cpu_dummy_writes_oam" {
            test {
                waitForFrame("f42bb12c717b42f17a8fc6a0b6177780")
            }
        }
        "cpu_dummy_writes_ppumem" {
            test {
                waitForFrame("f75d08d8a784a5f118d9f3291742791c")
            }
        }
        "cpu_exec_space_apu" {
            test {
                waitForFrame("4d5a451ee38ae0f36cd68e8c833dc4de")
            }
        }
        "cpu_exec_space_ppuio" {
            test {
                waitForFrame("f96dad52bedbecf664ae55dac70e13cc")
            }
        }
        "cpu_flag_concurrency" {
            test {
                waitForFrame("e73d15787ec628ae47a02d1d07d0c4bf")
            }
        }
        "cpu_interrupts_v2" {
            test {
                waitForFrame("bcc7fd05ce74059f799c5e98187ad318")
            }
        }
        "cpu_timing" {
            test {
                waitForFrame("6eb0ca2fa2f80aad378b6310d0ea4f6d")
            }
        }
        "instr_misc" {
            test {
                waitForFrame("90cae66096dbbb82482ae63ce0046d8c")
            }
        }
        "instr_test_v5/all_instrs" {
            test {
                waitForFrame("3b93e13482d21036a79e5aae2e6671ff")
            }
        }
        "instr_test_v5/official_only" {
            test {
                waitForFrame("3b93e13482d21036a79e5aae2e6671ff")
            }
        }
        "instr_timing" {
            test {
                waitForFrame("e2ad239e68dfe0c3c890eeec3024cc19")
            }
        }
        "dmc_dma_during_read4/dma_2007_read" {
            test {
                waitForFrame("a3ea77924b5da631b47a1cb327795edd")
            }
        }
        "dmc_dma_during_read4/dma_2007_write" {
            test {
                waitForFrame("0b1636d65b3ada4e083461311bff04f1")
            }
        }
        "dmc_dma_during_read4/dma_4016_read" {
            test(autoStart = false) {
                console.settings.port1.configureStandardControllerForThisPort()
                waitForFrame("9c8331e39710bdca6c495754e397d9ef")
            }
        }
        "dmc_dma_during_read4/double_2007_read" {
            test {
                waitForFrame("fd119f938ad5ecee905e4e442bf0c2a6")
            }
        }
        "dmc_dma_during_read4/read_write_2007" {
            test {
                waitForFrame("2e80cc410b964277e1ca6d3967b696be")
            }
        }
        "sprdma_and_dmc_dma" {
            test {
                waitForFrame("73dc14add4d4d382378ba45e9505c0e4")
            }
        }
        "sprdma_and_dmc_dma_512" {
            test {
                waitForFrame("cdec9cdf1ef56cad9a6cfd5494a05bc9")
            }
        }
        "cpu_reset/ram_after_reset" {
            test {
                delay(3000)
                softReset()
                waitForFrame("df699dbfd9bf60fc2aa185edf4ba78df")
            }
        }
        "cpu_reset/registers" {
            test {
                delay(3000)
                softReset()
                waitForFrame("0d6c3522629676ad653242dc2a6b66c0")
            }
        }
        "nestest" {
            test(autoStart = false) {
                console.settings.port1.configureStandardControllerForThisPort()
                start()
                delay(1000)
                pressAndRelease(START)
                waitForFrame("fd3c95a9bc606c483b7922b58a5ee86c")
            }
        }
        "blargg_cpu" {
            test {
                waitForFrame("fd7aaefa3f905184218cf3a6034b556c")
            }
        }
    }
}
