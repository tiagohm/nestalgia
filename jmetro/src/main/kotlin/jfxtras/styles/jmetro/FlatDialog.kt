package jfxtras.styles.jmetro

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.ListChangeListener
import javafx.scene.control.Dialog
import javafx.scene.image.Image
import javafx.stage.Stage

class FlatDialog<R> : Dialog<R>() {

    val iconlessProperty: BooleanProperty = SimpleBooleanProperty(true)

    private var isResettingIcon = false

    init {
        initDialog(this, iconless)
        setupDialogIconsListener()
    }

    var iconless
        get() = iconlessProperty.get()
        set(value) {
            iconlessProperty.set(value)
        }

    private fun setupDialogIconsListener() {
        val stage = dialogPane.scene.window as Stage
        stage.icons.addListener(::dialogIconsChanged)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun dialogIconsChanged(change: ListChangeListener.Change<out Image>) {
        // When initOwner is called on the Dialog the icon
        // is changed and this event is fired.
        if (isResettingIcon) {
            return
        }

        // When there is an initOwner call the Dialog icon gets reset,
        // we need to reapply the JMetro icon.
        isResettingIcon = true
        initDialogIcon(this, iconless)
        isResettingIcon = false
    }

    companion object {

        fun initDialog(dialog: Dialog<*>, iconless: Boolean) {
            dialog.title = ""
            initDialogIcon(dialog, iconless)
        }

        fun initDialogIcon(dialog: Dialog<*>, iconless: Boolean) {
            if (!iconless) return
            val stage = dialog.dialogPane.scene.window as Stage
            val whiteIcon = Image(Thread.currentThread().contextClassLoader.getResource("whiteIcon.png")!!.toExternalForm())
            stage.icons.add(whiteIcon)
        }
    }
}
