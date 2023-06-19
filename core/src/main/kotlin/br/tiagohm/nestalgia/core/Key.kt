package br.tiagohm.nestalgia.core

import java.io.Serializable

interface Key : Serializable {

    val code: Int

    companion object {

        @JvmStatic val UNDEFINED: Key = KeyboardKeys.UNDEFINED

        @JvmStatic
        fun of(code: Int): Key = if (code == 0) UNDEFINED else KeyboardKeys.MAPPED_ENTRIES[code]!!
    }
}
