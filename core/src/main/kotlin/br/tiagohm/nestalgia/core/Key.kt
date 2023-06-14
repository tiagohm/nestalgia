package br.tiagohm.nestalgia.core

interface Key {

    val code: Int

    companion object {

        @JvmStatic val UNDEFINED: Key = KeyboardKeys.UNDEFINED

        @JvmStatic
        fun of(code: Int): Key = if (code == 0) UNDEFINED else KeyboardKeys.MAPPED_ENTRIES[code]!!
    }
}
