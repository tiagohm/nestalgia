package br.tiagohm.nestalgia.desktop.gui

import br.tiagohm.nestalgia.desktop.gui.home.HomeWindow
import br.tiagohm.nestalgia.desktop.helper.resource
import br.tiagohm.nestalgia.desktop.helper.resourceUrl
import jakarta.annotation.PostConstruct
import javafx.application.Platform
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.Scene
import javafx.scene.image.Image
import javafx.stage.Modality
import javafx.stage.Stage
import jfxtras.styles.jmetro.JMetro
import jfxtras.styles.jmetro.JMetroStyleClass
import jfxtras.styles.jmetro.Style
import java.util.concurrent.atomic.AtomicBoolean

abstract class AbstractWindow : AutoCloseable {

    protected abstract val resourceName: String

    protected open val window by lazy { Stage() }

    private val showingAtFirstTime = AtomicBoolean(true)

    @PostConstruct
    protected fun setUp() {
        window.setOnShowing {
            if (showingAtFirstTime.compareAndSet(true, false)) {
                val loader = FXMLLoader(resourceUrl("screens/$resourceName.fxml")!!)
                loader.setController(this)
                val root = loader.load<Parent>()

                val scene = Scene(root)
                window.scene = scene
                window.icons.add(Image(resource("icons/mario-256.png")))

                JMetro(scene, Style.DARK)
                root.styleClass.add(JMetroStyleClass.BACKGROUND)
                root.stylesheets.add("css/Global.css")

                onCreate()
            }
        }

        window.setOnShown { onStart() }

        window.setOnHiding {
            onStop()

            if (this@AbstractWindow is HomeWindow) {
                onClose()
            }
        }
    }

    protected open fun onCreate() = Unit

    protected open fun onStart() = Unit

    protected open fun onStop() = Unit

    protected open fun onClose() = Unit

    var resizable
        get() = window.isResizable
        set(value) {
            window.isResizable = value
        }

    var maximized
        get() = window.isMaximized
        set(value) {
            window.isMaximized = value
        }

    val showing
        get() = window.isShowing

    val initialized
        get() = !showingAtFirstTime.get()

    var title
        get() = window.title!!
        set(value) {
            window.title = value
        }

    var x
        get() = window.x
        set(value) {
            window.x = value
        }

    var y
        get() = window.y
        set(value) {
            window.y = value
        }

    var width
        get() = window.width
        set(value) {
            window.width = value
        }

    var height
        get() = window.height
        set(value) {
            window.height = value
        }

    val sceneWidth
        get() = window.scene.width

    val sceneHeight
        get() = window.scene.height

    val borderSize
        get() = (width - sceneWidth) / 2.0

    val titleHeight
        get() = (height - sceneHeight) - borderSize

    fun show(
        requestFocus: Boolean = false,
        bringToFront: Boolean = false,
    ) {
        window.show()

        if (requestFocus) window.requestFocus()
        if (bringToFront) window.toFront()
    }

    fun showAndWait(owner: AbstractWindow? = null) {
        if (window.owner == null) {
            window.initModality(Modality.WINDOW_MODAL)
            if (owner is AbstractWindow && owner !== this) window.initOwner(owner.window)
        }

        window.showAndWait()
    }

    override fun close() {
        if (Platform.isFxApplicationThread()) {
            window.close()
        }
    }
}
