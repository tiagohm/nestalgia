package br.tiagohm.nestalgia.core

import br.tiagohm.nestalgia.core.MirroringType.*
import org.slf4j.LoggerFactory
import java.io.IOException

object UnifLoader {

    fun load(rom: ByteArray, name: String) = read(rom, name)

    private fun read(rom: ByteArray, name: String): RomData {
        // Skip header, version & null bytes, start reading at first chunk
        var offset = 32

        fun readFourCC() = String(rom, offset, 4).also { offset += 4 }

        fun readByte() = rom[offset++].toUnsignedInt()

        fun readInt() = (rom[offset++].toUnsignedInt()) or
            (rom[offset++].toUnsignedInt() shl 8) or
            (rom[offset++].toUnsignedInt() shl 16) or
            (rom[offset++].toUnsignedInt() shl 24)

        fun readString(): String {
            val res = StringBuilder()

            while (offset < rom.size) {
                // End of string
                if (rom[offset] == 0.toByte()) {
                    offset++
                    break
                } else {
                    // Ignore spaces
                    res.append(rom[offset++].toUnsignedInt().toChar())
                }
            }

            return "$res"
        }

        var board = ""
        var mapperId = 0
        var system = GameSystem.UNKNOWN
        var hasBattety = false
        var mirroring = HORIZONTAL
        val prgChunks = Array(16) { ByteArray(0) }
        val chrChunks = Array(16) { ByteArray(0) }

        while (offset < rom.size) {
            // FourCC + Length
            if (offset + 8 > rom.size) {
                break
            }

            val fourCC = readFourCC()
            val length = readInt()

            if (offset + length > rom.size) {
                break
            }

            when {
                // Mapper
                fourCC == "MAPR" -> {
                    board = readString()

                    if (board.isNotEmpty()) {
                        // println("UNIF Board: $board")

                        mapperId = mapperId(board)

                        if (mapperId == UnifBoard.UNKNOWN.id) {
                            LOG.error("[UNIF] ERROR: Unknown board: {}", board)
                        }
                    } else {
                        throw IOException("[UNIF]: Invalid UNIF board name")
                    }

                    if (board.length + 1 < length) {
                        offset += length - board.length - 1
                    }
                }
                // PRG
                fourCC.startsWith("PRG") -> {
                    val chunkNumber = fourCC[3].toString().toInt(16)
                    prgChunks[chunkNumber] = ByteArray(length)
                    rom.copyInto(prgChunks[chunkNumber], 0, offset, offset + length)
                    offset += length
                }
                // CHR
                fourCC.startsWith("CHR") -> {
                    val chunkNumber = fourCC[3].toString().toInt(16)
                    chrChunks[chunkNumber] = ByteArray(length)
                    rom.copyInto(chrChunks[chunkNumber], 0, offset, offset + length)
                    offset += length
                }
                // System
                fourCC == "TVCI" -> {
                    system = if (readByte() == 1) GameSystem.PAL else GameSystem.NTSC
                }
                //
                // Battery
                fourCC == "BATR" -> {
                    hasBattety = readByte() > 0
                }
                // Mirroring Type
                fourCC == "MIRR" -> {
                    mirroring = when (readByte()) {
                        1 -> VERTICAL
                        2 -> SCREEN_A_ONLY
                        3 -> SCREEN_B_ONLY
                        4 -> FOUR_SCREENS
                        else -> HORIZONTAL
                    }
                }
                // Controller
                fourCC == "CTRL" -> {
                    readByte()
                }
                // Dump info
                fourCC == "DINF" -> {
                    offset += 204
                }
                // Name
                fourCC == "NAME" -> {
                    readString()
                }
                // Unused: PCKn, CCKn, WRTR, READ, VROR
                else -> {
                    offset += length
                }
            }
        }

        val prgRom = IntArray(prgChunks.sumOf { it.size })
        val chrRom = IntArray(chrChunks.sumOf { it.size })

        var prgRomOffset = 0
        var chrRomOffset = 0

        for (i in prgChunks.indices) {
            prgChunks[i].forEach { prgRom[prgRomOffset++] = it.toUnsignedInt() }
            chrChunks[i].forEach { chrRom[chrRomOffset++] = it.toUnsignedInt() }
        }

        if (prgRom.isEmpty()) {
            throw IOException("[UNIF]: PRG ROM is empty")
        }

        val prgChr = IntArray(prgRom.size + chrRom.size)
        var romOffset = 0

        prgRom.forEach { prgChr[romOffset++] = it }
        chrRom.forEach { prgChr[romOffset++] = it }

        val hash = HashInfo(
            prgRom.crc32(), prgRom.md5(), prgRom.sha1(), prgRom.sha256(),
            chrRom.crc32(), chrRom.md5(), chrRom.sha1(), chrRom.sha256(),
            prgChr.crc32(), prgChr.md5(), prgChr.sha1(), prgChr.sha256(),
        )

        // val db = GameDatabase[hash.crc32]

        val info = RomInfo(
            name,
            RomFormat.UNIF,
            mapperId = mapperId,
            system = system,
            hasBattery = hasBattety,
            mirroring = mirroring,
            hash = hash,
            unifBoard = board,
            // gameInfo = db,
        )

        return RomData(
            info,
            prgRom = prgRom,
            chrRom = chrRom,
            rawData = rom,
        )

        // return db?.update(data, false) ?: data
    }

    fun mapperId(board: String): Int {
        val name = when (board.substring(0, 4)) {
            "NES-",
            "UNL-",
            "HVC-",
            "BTL-",
            "BMC-" -> board.substring(4)
            else -> board
        }

        return BOARDS[name] ?: UnifBoard.UNKNOWN.id
    }

    val BOARDS = mapOf(
        "11160" to 299,
        "12-IN-1" to 331,
        "13in1JY110" to 295,
        "190in1" to 300,
        "22211" to 132,
        "255in1" to UnifBoard.UNL_255_IN_1.id, // Doesn't actually exist as a UNIF file (used to assign a mapper to the 255-in-1 rom)
        "3D-BLOCK" to UnifBoard.UNKNOWN.id,
        "411120-C" to 287,
        "42in1ResetSwitch" to 233,
        "43272" to 227,
        "603-5052" to 238,
        "64in1NoRepeat" to 314,
        "70in1" to 236,
        "70in1B" to 236,
        "810544-C-A1" to 261,
        "830425C-4391T" to 320,
        "8157" to 301,
        "8237" to 215,
        "8237A" to UnifBoard.UNL_8237A.id,
        "830118C" to 348,
        "A65AS" to 285,
        "AC08" to UnifBoard.AC08.id,
        "ANROM" to 7,
        "AX5705" to 530,
        "BB" to 108,
        "BS-5" to 286,
        "CC-21" to UnifBoard.CC21.id,
        "CITYFIGHT" to 266,
        "COOLBOY" to 268,
        "CNROM" to 3,
        "CPROM" to 13,
        "D1038" to 59,
        "DANCE" to UnifBoard.UNKNOWN.id,
        "DANCE2000" to 518,
        "DREAMTECH01" to 521,
        "EDU2000" to 329,
        "EKROM" to 5,
        "ELROM" to 5,
        "ETROM" to 5,
        "EWROM" to 5,
        "FARID_SLROM_8-IN-1" to 323,
        "FARID_UNROM_8-IN-1" to 324,
        "FK23C" to 176,
        "FK23CA" to 176,
        "FS304" to 162,
        "G-146" to 349,
        "GK-192" to 58,
        "GS-2004" to 283,
        "GS-2013" to 283,
        "Ghostbusters63in1" to 226,
        "H2288" to 123,
        "HKROM" to 4,
        "KOF97" to 263,
        "KONAMI-QTAI" to 190,
        "K-3046" to 336,
        "KS7010" to UnifBoard.UNKNOWN.id,
        "KS7012" to 346,
        "KS7013B" to 312,
        "KS7016" to 306,
        "KS7017" to 303,
        "KS7030" to 347,
        "KS7031" to 305,
        "KS7032" to 142,
        "KS7037" to 307,
        "KS7057" to 302,
        "LE05" to UnifBoard.UNKNOWN.id,
        "LH10" to 522,
        "LH32" to 125,
        "LH51" to 309,
        "LH53" to 535,
        "MALISB" to 325,
        "MARIO1-MALEE2" to 55,
        "MHROM" to 66,
        "N625092" to 221,
        "NROM" to 0,
        "NROM-128" to 0,
        "NROM-256" to 0,
        "NTBROM" to 68,
        "NTD-03" to 290,
        "NovelDiamond9999999in1" to 201,
        "OneBus" to UnifBoard.UNKNOWN.id,
        "PEC-586" to UnifBoard.UNKNOWN.id,
        // Doesn't actually exist as a UNIF file (used to reassign a new mapper number to the Puzzle beta).
        "PUZZLE" to UnifBoard.UNL_PUZZLE.id,
        "RESET-TXROM" to 313,
        "RET-CUFROM" to 29,
        "RROM" to 0,
        "RROM-128" to 0,
        "SA-002" to 136,
        "SA-0036" to 149,
        "SA-0037" to 148,
        "SA-009" to 160,
        "SA-016-1M" to 146,
        "SA-72007" to 145,
        "SA-72008" to 133,
        "SA-9602B" to 513,
        "SA-NROM" to 143,
        "SAROM" to 1,
        "SBROM" to 1,
        "SC-127" to 35,
        "SCROM" to 1,
        "SEROM" to 1,
        "SGROM" to 1,
        "SHERO" to 262,
        "SKROM" to 1,
        "SL12" to 116,
        "SL1632" to 14,
        "SL1ROM" to 1,
        "SLROM" to 1,
        "SMB2J" to 304,
        "SNROM" to 1,
        "SOROM" to 1,
        "SSS-NROM-256" to UnifBoard.SSS_NROM_256.id,
        "SUNSOFT_UNROM" to 93,
        "Sachen-74LS374N" to 150,
        "Sachen-74LS374NA" to 243,
        "Sachen-8259A" to 141,
        "Sachen-8259B" to 138,
        "Sachen-8259C" to 139,
        "Sachen-8259D" to 137,
        "Super24in1SC03" to 176,
        "SuperHIK8in1" to 45,
        "Supervision16in1" to 53,
        "T-227-1" to UnifBoard.UNKNOWN.id,
        "T-230" to 529,
        "T-262" to 265,
        "TBROM" to 4,
        "TC-U01-1.5M" to 147,
        "TEK90" to 90,
        "TEROM" to 4,
        "TF1201" to 298,
        "TFROM" to 4,
        "TGROM" to 4,
        "TKROM" to 4,
        "TKSROM" to 4,
        "TLROM" to 4,
        "TLSROM" to 4,
        "TQROM" to 4,
        "TR1ROM" to 4,
        "TSROM" to 4,
        "TVROM" to 4,
        "Transformer" to UnifBoard.UNKNOWN.id,
        "UNROM" to 2,
        "UNROM-512-8" to 30,
        "UNROM-512-16" to 30,
        "UNROM-512-32" to 30,
        "UOROM" to 2,
        "VRC7" to 85,
        "YOKO" to 264,
        "SB-2000" to UnifBoard.UNKNOWN.id,
        "158B" to 258,
        "DRAGONFIGHTER" to 292,
        "EH8813A" to 519,
        "HP898F" to 319,
        "F-15" to 259,
        "RT-01" to 328,
        "81-01-31-C" to UnifBoard.UNKNOWN.id,
        "8-IN-1" to 333,
        "WS" to 332,
        "80013-B" to 274,
        "WAIXING-FW01" to 227,
        "WAIXING-FS005" to 176,
        "HPxx" to 260,
        "HP2018A" to 260,
        "DRIPGAME" to 284,
        "60311C" to 289,
        "CHINA_ER_SAN2" to 19, //Appears to be a mapper 19 hack specific for VirtuaNES (which adds chinese text on top of the PPU's output), unknown if a board actually exists
        "STREETFIGTER-GAME4IN1" to 49,
        "HP2018-A" to 260,
        "JC-016-2" to 205,
        "NEWSTAR-GRM070-8IN1" to 333,
        "S-2009" to 434,
        "10-24-C-A1" to 327,
        "830134C" to 315,
        "CTC-09" to 335,
        "GN-26" to 344,
        "K-3006" to 339,
        "K-3033" to 322,
        "K-3036" to 340,
        "L6IN1" to 345,
        "SA005-A" to 338,
        "TJ-03" to 341,
        "TH2131-1" to 308,
        "900218" to 524,
        "831128C" to 528,
        "BJ-56" to 526,
        "KS7021A" to 525,
        "MINDKIDS" to 268,
        "KS106C" to 352,
        "RESETNROM-XIN1" to 343,
        "T4A54A" to 134,
        "WX-KB4K" to 134,
        "BS-5652" to 134,
        "891227" to 350,
        "CTC-12IN1" to 337,
        "BS-110" to 444,
        "830752C" to 396,
        "SB-5013" to 359,
        "82112C" to 540,
        "22026" to 271,
        "N49C-300" to 369,
        "K-3010" to 438,
        "K-3071" to 438,
        "AX-40G" to 527,
        "DS-07" to 439,
        "K86B" to 439,
    )

    private val LOG = LoggerFactory.getLogger(UnifLoader::class.java)
}
