package br.tiagohm.nestalgia.core

interface Peekable {
    fun peek(addr: UShort): UByte = 0U

    fun peekWord(addr: UShort): UShort {
        return makeUShort(peek(addr), peek(addr.plusOne()))
    }
}