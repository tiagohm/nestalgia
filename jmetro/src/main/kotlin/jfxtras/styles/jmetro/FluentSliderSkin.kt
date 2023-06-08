package jfxtras.styles.jmetro

import javafx.geometry.Orientation
import javafx.scene.chart.NumberAxis
import javafx.scene.control.Slider
import javafx.scene.control.skin.SliderSkin
import javafx.scene.layout.StackPane
import kotlin.math.max

class FluentSliderSkin(slider: Slider) : SliderSkin(slider) {

    private val fill = StackPane()
    private val thumb = skinnable.lookup(".thumb") as StackPane
    private val track = skinnable.lookup(".track") as StackPane
    private val trackToTickGap = 2.0

    init {
        fill.styleClass.add("fill")

        children.add(children.indexOf(track) + 1, fill)

        fill.eventDispatcher = track.eventDispatcherProperty().get()

        registerChangeListener(slider.showTickMarksProperty()) { thickMarksChanged() }
        registerChangeListener(slider.showTickLabelsProperty()) { thickMarksChanged() }
    }

    private fun thickMarksChanged() {
        children.add(children.indexOf(track) + 1, fill)
    }

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)

        val control = skinnable
        val showTickMarks = control.isShowTickMarks || control.isShowTickLabels
        val thumbWidth = snapSizeX(thumb.prefWidth(-1.0))
        val thumbHeight = snapSizeY(thumb.prefHeight(-1.0))

        val trackRadius = if (track.background == null) 0.0
        else if (track.background.fills.size > 0) track.background.fills[0].radii.topLeftHorizontalRadius
        else 0.0

        val numberAxis = control.lookup("NumberAxis") as? NumberAxis

        if (skinnable.orientation == Orientation.HORIZONTAL) {
            val tickLineHeight = if (showTickMarks) numberAxis?.prefHeight(-1.0) ?: 0.0 else 0.0
            val trackHeight = snapSizeY(track.prefHeight(-1.0))
            val trackAreaHeight = max(trackHeight, thumbHeight)
            val totalHeightNeeded = trackAreaHeight + if (showTickMarks) trackToTickGap + tickLineHeight else 0.0
            val startY = y + (h - totalHeightNeeded) / 2
            val trackStart = snapPositionX(x + thumbWidth / 2)
            val trackTop = (startY + (trackAreaHeight - trackHeight) / 2).toInt().toDouble()

            fill.resizeRelocate(
                (trackStart - trackRadius).toInt().toDouble(),
                trackTop,
                trackStart.toInt() - trackRadius + thumb.layoutX,
                trackHeight
            )
        } else {
            val tickLineWidth = if (showTickMarks) numberAxis?.prefWidth(-1.0) ?: 0.0 else 0.0
            val trackWidth = snapSizeX(track.prefWidth(-1.0))
            val trackAreaWidth = max(trackWidth, thumbWidth)
            val totalWidthNeeded = trackAreaWidth + if (showTickMarks) trackToTickGap + tickLineWidth else 0.0
            val startX = x + (w - totalWidthNeeded) / 2
            val trackLength = snapSizeY(h - thumbHeight)
            val trackStart = snapPositionY(y + thumbHeight / 2)
            val trackLeft = (startX + (trackAreaWidth - trackWidth) / 2).toInt().toDouble()

            fill.resizeRelocate(
                trackLeft,
                trackStart.toInt() - trackRadius + thumb.layoutY,
                trackWidth,
                trackLength - thumb.layoutY
            )
        }
    }
}
