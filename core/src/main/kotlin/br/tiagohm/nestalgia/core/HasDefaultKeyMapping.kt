package br.tiagohm.nestalgia.core

sealed interface HasDefaultKeyMapping {

    fun defaultKeyMapping() = KeyMapping().also(::populateWithDefault)

    fun populateWithDefault(keyMapping: KeyMapping)
}
