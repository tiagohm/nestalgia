package jfxtras.styles.jmetro

import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.collections.ObservableList
import javafx.scene.Parent
import javafx.scene.Scene
import jfxtras.styles.jmetro.Style.*

class JMetro() {

    val sceneProperty: ObjectProperty<Scene> = object : SimpleObjectProperty<Scene>() {

        override fun invalidated() {
            if (get() == null) return
            parentProperty.set(null)
            reApplyTheme()
        }
    }

    val parentProperty: ObjectProperty<Parent> = object : SimpleObjectProperty<Parent>() {

        override fun invalidated() {
            if (get() == null) return
            sceneProperty.set(null)
            JMetroStyleClass.addIfNotPresent(get().styleClass, "root")
            reApplyTheme()
        }
    }

    val styleProperty: ObjectProperty<Style> = object : SimpleObjectProperty<Style>(LIGHT) {

        override fun invalidated() {
            reApplyTheme()
        }
    }

    val overridingStylesheets: ObservableList<String> = FXCollections.observableArrayList()

    init {
        overridingStylesheets.addListener(::overridingStylesheetsChanged)
    }

    constructor(style: Style) : this() {
        styleProperty.set(style)
    }

    constructor(scene: Scene, style: Style) : this() {
        styleProperty.set(style)
        sceneProperty.set(scene)
    }

    constructor(parent: Parent, style: Style) : this() {
        styleProperty.set(style)
        parentProperty.set(parent)
    }

    fun reApplyTheme() {
        val stylesheetsList = appliedStylesheetsList ?: return

        // Remove existing JMetro style stylesheets that are configurable.
        stylesheetsList.remove(LIGHT.styleStylesheetURL)
        stylesheetsList.remove(DARK.styleStylesheetURL)
        var baseStylesheetIndex = stylesheetsList.indexOf(BASE_STYLESHEET_URL)

        // Add BASE_STYLESHEET before all other JMetro stylesheets.
        if (baseStylesheetIndex == -1) {
            // There are no base stylesheets added yet.
            addBaseStylesheets(stylesheetsList)
            // This needs to be added after base stylesheet so that specific,
            // overriding styles here are applied.
            stylesheetsList.add(style.styleStylesheetURL)
        } else {
            // Base stylesheets were already added.
            stylesheetsList.add(++baseStylesheetIndex, style.styleStylesheetURL)
        }
    }

    private fun addBaseStylesheets(stylesheetsList: ObservableList<String>) {
        stylesheetsList.add(BASE_STYLESHEET_URL)
        stylesheetsList.add(BASE_EXTRAS_STYLESHEET_URL)
        stylesheetsList.add(BASE_OTHER_LIBRARIES_STYLESHEET_URL)
    }

    private fun overridingStylesheetsChanged(changed: ListChangeListener.Change<out String>) {
        val stylesheetsListBeingApplied =
            requireNotNull(appliedStylesheetsList) { "Scene and Parent can't be null, they must be set by the programmer" }

        // Currently this only supports adding and removing of stylesheets
        // of the overriding stylesheets list.
        while (changed.next()) {
            if (changed.wasRemoved()) {
                for (stylesheetURL in changed.removed) {
                    stylesheetsListBeingApplied.remove(stylesheetURL)
                }
            }

            if (changed.wasAdded()) {
                // For now, we just add at the bottom of the list
                stylesheetsListBeingApplied.addAll(changed.addedSubList)
            }
        }
    }

    private val appliedStylesheetsList
        get() = scene?.stylesheets ?: parent?.stylesheets

    var parent: Parent?
        get() = parentProperty.get()
        set(value) {
            parentProperty.set(value)
        }

    var scene: Scene?
        get() = sceneProperty.get()
        set(value) {
            sceneProperty.set(value)
        }

    var style: Style
        get() = styleProperty.get()
        set(value) {
            styleProperty.set(value)
        }

    companion object {

        private val BASE_STYLESHEET_URL =
            Thread.currentThread().contextClassLoader.getResource("base.css")!!.toExternalForm()
        private val BASE_OTHER_LIBRARIES_STYLESHEET_URL =
            Thread.currentThread().contextClassLoader.getResource("base-other-libraries.css")!!.toExternalForm()
        private val BASE_EXTRAS_STYLESHEET_URL =
            Thread.currentThread().contextClassLoader.getResource("base-extras.css")!!.toExternalForm()
    }
}
