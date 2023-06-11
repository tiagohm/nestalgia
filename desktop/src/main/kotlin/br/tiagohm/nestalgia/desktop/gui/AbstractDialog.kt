package br.tiagohm.nestalgia.desktop.gui

abstract class AbstractDialog : AbstractWindow() {

    var saved = false
        protected set

    override fun onStart() {
        saved = false
    }
}
