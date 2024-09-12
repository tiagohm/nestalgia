package jfxtras.styles.jmetro

import javafx.beans.InvalidationListener
import javafx.beans.property.BooleanProperty
import javafx.css.CssMetaData
import javafx.css.SimpleStyleableBooleanProperty
import javafx.css.Styleable
import javafx.css.StyleableProperty
import javafx.css.converter.BooleanConverter
import javafx.event.EventHandler
import javafx.geometry.HPos
import javafx.geometry.VPos
import javafx.scene.control.SkinBase
import javafx.scene.control.TextField
import javafx.scene.control.skin.TextFieldSkin
import javafx.scene.input.MouseEvent
import javafx.scene.layout.Region
import javafx.scene.layout.StackPane
import java.util.*

open class TextFieldWithButtonSkin(protected var textField: TextField) : TextFieldSkin(textField) {

    private val textChanged = InvalidationListener { onTextChanged() }
    private val focusChanged = InvalidationListener { onFocusChanged() }
    private val rightButtonVisibleChanged = InvalidationListener { onRightButtonVisibilityChanged() }
    private val rightButton = StackPane()
    private val rightButtonGraphic = Region()

    val rightButtonVisibleProperty: BooleanProperty = SimpleStyleableBooleanProperty(RIGHT_BUTTON_VISIBLE_META_DATA, true)

    val rightButtonVisible
        get() = rightButtonVisibleProperty.get()

    init {
        rightButton.isManaged = false
        rightButton.styleClass.setAll("right-button")
        rightButton.isFocusTraversable = false

        rightButtonGraphic.styleClass.setAll("right-button-graphic")
        rightButtonGraphic.isFocusTraversable = false
        rightButtonGraphic.maxWidth = Region.USE_PREF_SIZE
        rightButtonGraphic.maxHeight = Region.USE_PREF_SIZE
        rightButtonGraphic.isVisible = false

        rightButton.isVisible = false
        rightButton.children.add(rightButtonGraphic)

        children.add(rightButton)

        setupListeners()
    }

    override fun getCssMetaData() = STYLEABLES

    private fun setupListeners() {
        rightButton.onMousePressed = EventHandler(::onRightButtonPressed)
        rightButton.onMouseReleased = EventHandler(::onRightButtonReleased)
        skinnable.textProperty().addListener(textChanged)
        skinnable.focusedProperty().addListener(focusChanged)
        rightButtonVisibleProperty.addListener(rightButtonVisibleChanged)
    }

    protected fun onTextChanged() {
        updateRightButtonVisibility()
    }

    protected fun onFocusChanged() {
        updateRightButtonVisibility()
    }

    protected fun onRightButtonVisibilityChanged() {
        updateRightButtonVisibility()
    }

    private fun updateRightButtonVisibility() {
        if (textField.text == null) return
        val hasFocus = textField.isFocused
        val isEmpty = textField.text.isEmpty()
        val isRightButtonVisible = rightButtonVisible
        val shouldBeVisible = isRightButtonVisible && hasFocus && !isEmpty
        rightButton.isVisible = shouldBeVisible
        rightButtonGraphic.isVisible = shouldBeVisible
    }

    protected open fun onRightButtonPressed(event: MouseEvent) = Unit

    protected open fun onRightButtonReleased(event: MouseEvent) = Unit

    override fun layoutChildren(x: Double, y: Double, w: Double, h: Double) {
        super.layoutChildren(x, y, w, h)

        val clearGraphicWidth = snapSizeX(rightButtonGraphic.prefWidth(-1.0))
        val clearButtonWidth = rightButton.snappedLeftInset() + clearGraphicWidth + rightButton.snappedRightInset()

        rightButton.resize(clearButtonWidth, h)

        positionInArea(
            rightButton,
            x + w - clearButtonWidth, y,
            clearButtonWidth, h, 0.0, HPos.CENTER, VPos.CENTER
        )
    }

    override fun dispose() {
        textField.textProperty().removeListener(textChanged)
        textField.focusedProperty().removeListener(focusChanged)
        rightButtonVisibleProperty.removeListener(rightButtonVisibleChanged)
        super.dispose()
    }

    companion object {

        private const val RIGHT_BUTTON_VISIBLE_PROPERTY_NAME = "-right-button-visible"

        private val RIGHT_BUTTON_VISIBLE_META_DATA = object : CssMetaData<TextField, Boolean>(
            RIGHT_BUTTON_VISIBLE_PROPERTY_NAME,
            BooleanConverter.getInstance(), true
        ) {
            override fun isSettable(textField: TextField): Boolean {
                val skin = textField.skin as TextFieldWithButtonSkin
                return !skin.rightButtonVisibleProperty.isBound
            }

            override fun getStyleableProperty(textField: TextField): StyleableProperty<Boolean> {
                val skin = textField.skin as TextFieldWithButtonSkin
                return skin.rightButtonVisibleProperty as SimpleStyleableBooleanProperty
            }
        }

        val STYLEABLES: List<CssMetaData<out Styleable, *>>

        init {
            val styleables = ArrayList(SkinBase.getClassCssMetaData())
            styleables.add(RIGHT_BUTTON_VISIBLE_META_DATA)
            styleables.addAll(getClassCssMetaData())
            STYLEABLES = Collections.unmodifiableList(styleables)
        }
    }
}
