package br.tiagohm.nestalgia.core

sealed interface Peekable {

    fun peek(addr: Int) = 0

    fun peekWord(addr: Int) = peek(addr) or (peek(addr + 1) shl 8)
}
