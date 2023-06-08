package br.tiagohm.nestalgia.desktop.gui

abstract class AbstractDialog : AbstractWindow() {

    @Volatile var saved = false
        protected set

    override fun onStart() {
        saved = false
    }
}
