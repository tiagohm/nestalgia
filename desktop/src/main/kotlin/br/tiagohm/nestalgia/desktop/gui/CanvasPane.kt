package br.tiagohm.nestalgia.desktop.gui

import javafx.scene.canvas.Canvas
import javafx.scene.layout.Pane

open class CanvasPane(width: Double, height: Double) : Pane() {

    constructor(width: Int, height: Int) : this(width.toDouble(), height.toDouble())

    @JvmField protected val canvas = Canvas(width, height)

    init {
        children.add(canvas)

        canvas.widthProperty().bind(widthProperty())
        canvas.heightProperty().bind(heightProperty())
    }
}
