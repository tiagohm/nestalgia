package br.tiagohm.nestalgia.ui.dialogs

import br.tiagohm.nestalgia.ui.APP_ICON
import java.awt.BorderLayout
import javax.swing.JDialog
import javax.swing.JPanel
import kotlin.math.max

abstract class Dialog(
    title: String,
    isModal: Boolean = true,
    val minWidth: Int = -1,
    val minHeight: Int = -1,
) : JDialog() {

    init {
        this.title = title
        this.isModal = isModal
        this.defaultCloseOperation = DISPOSE_ON_CLOSE
        this.setIconImage(APP_ICON)
    }

    fun initialize() {
        contentPane = JPanel().also { it.layout = BorderLayout() }
        updateView()
    }

    abstract val body: JPanel

    protected fun updateView() {
        contentPane.removeAll()
        contentPane.add(body, BorderLayout.CENTER)

        pack()
        validate()

        setSize(max(width, minWidth), max(height, minHeight))
    }
}