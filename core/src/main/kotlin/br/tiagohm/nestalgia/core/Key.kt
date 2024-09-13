package br.tiagohm.nestalgia.core

import java.io.Serializable

sealed interface Key : Serializable {

    val code: Int

    companion object {

        val UNDEFINED: Key = KeyboardKeys.UNDEFINED

        fun of(code: Int): Key = if (code == 0) UNDEFINED else KeyboardKeys.MAPPED_ENTRIES[code]!!
    }
}
