package jfxtras.styles.jmetro

import javafx.event.EventHandler
import javafx.scene.control.Spinner
import javafx.scene.control.skin.SpinnerSkin
import javafx.scene.input.ScrollEvent

class FluentSpinnerSkin(spinner: Spinner<Double>) : SpinnerSkin<Double>(spinner) {

    private val onMouseScroll = EventHandler<ScrollEvent> {
        if (it.deltaY > 0.0) skinnable.increment()
        else if (it.deltaY < 0.0) skinnable.decrement()
    }

    init {
        skinnable.addEventHandler(ScrollEvent.SCROLL, onMouseScroll)
    }

    override fun dispose() {
        skinnable.removeEventHandler(ScrollEvent.SCROLL, onMouseScroll)
        super.dispose()
    }
}
