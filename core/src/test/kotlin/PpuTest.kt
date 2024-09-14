import br.tiagohm.nestalgia.core.Region.PAL

class PpuTest : NesTesterSpec() {

    init {
        "ppu_read_buffer" {
            test {
                waitForFrame("c3b0cf089f8bbbe3016873c0a8d68129")
            }
        }
        "ppu_open_bus" {
            test {
                waitForFrame("784685618e2b8b02803496dfbe27a8b1")
            }
        }
        "ppu_vbl_nmi" {
            test {
                waitForFrame("d1e95eb6f7f86ea6bced614ab6895171")
            }
        }
        "sprite_overflow/basics" {
            test {
                waitForFrame("613e3de7aa1271b3a3846c4f5113411d")
            }
        }
        "sprite_overflow/details" {
            test {
                waitForFrame("5aefa9b36c3dc229b154ff95b2811cee")
            }
        }
        "sprite_overflow/timing" {
            test {
                waitForFrame("6246479f213754b0a481078ff978ebfd")
            }
        }
        "sprite_overflow/obscure" {
            test {
                waitForFrame("c99517bd98129e4a34e534e23e65bfe0")
            }
        }
        "sprite_overflow/emulator" {
            test {
                waitForFrame("77027c3cb80e6f7fde70f90db41a9348")
            }
        }
        "sprite_hit/basics" {
            test {
                waitForFrame("2f03c8c98886aa7a6b6deed881d5cd03")
            }
        }
        "sprite_hit/alignment" {
            test {
                waitForFrame("c2ea8dddf3292caa154846c04a1ca865")
            }
        }
        "sprite_hit/corners" {
            test {
                waitForFrame("101ea96bb2b65eb15973d89d86241099")
            }
        }
        "sprite_hit/flip" {
            test {
                waitForFrame("73fbc903c24453f026a8f7735167fb0b")
            }
        }
        "sprite_hit/left_clip" {
            test {
                waitForFrame("feffb80b28bb5ba7c50d3771a25e68b7")
            }
        }
        "sprite_hit/right_edge" {
            test {
                waitForFrame("ecead9b9ed9ccfdfbe60fb3ea3280682")
            }
        }
        "sprite_hit/screen_bottom" {
            test {
                waitForFrame("57dcde8fbcd4bdfa67be2634a0ddc128")
            }
        }
        "sprite_hit/double_height" {
            test {
                waitForFrame("811c0db671c1224d1b93548e6c2a7d4d")
            }
        }
        "sprite_hit/timing_basics" {
            test {
                waitForFrame("fedc60deada154d4e288046dd7f66023")
            }
        }
        "sprite_hit/timing_order" {
            test {
                waitForFrame("322e5f402fe41d1fdfc1d404bed52fd3")
            }
        }
        "sprite_hit/edge_timing" {
            test {
                waitForFrame("0104d0eea6ea2a0c5f2c3f76c973cc0c")
            }
        }
        "nmi_sync/ntsc" {
            test {
                waitForFrame("bc0ea217ff7f915c37ec23431e34c62d")
            }
        }
        "nmi_sync/pal" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("f96995c17ee7c608d1f07bbda03e44c7")
            }
        }
        "blargg_ppu/palette_ram" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_ppu/power_up_palette" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_ppu/sprite_ram" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_ppu/vbl_clear_time" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "blargg_ppu/vram_access" {
            test {
                waitForFrame("bfab9bf36408d0de87beef6f0da7cc2c")
            }
        }
        "palette" {
            test {
                waitForFrame("4b3fb5cc73c2237f3c07b1c8039a9137")
            }
        }
        "palette_pal" {
            test(autoStart = false) {
                console.settings.region = PAL
                waitForFrame("4b3fb5cc73c2237f3c07b1c8039a9137")
            }
        }
        "oam_read" {
            test {
                waitForFrame("28ef1eb13b55e52c4e892410b6903543")
            }
        }
        "oam_stress" {
            test {
                waitForFrame("5910d9310118fca1811d9349b38aca7f")
            }
        }
    }
}
