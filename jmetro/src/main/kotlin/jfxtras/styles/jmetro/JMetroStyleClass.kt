package jfxtras.styles.jmetro

object JMetroStyleClass {

    const val BACKGROUND = "background"
    const val UNDERLINE_TAB_PANE = "underlined"
    const val LIGHT_BUTTONS = "light"
    const val ALTERNATING_ROW_COLORS = "alternating-row-colors"
    const val TABLE_GRID_LINES = "column-grid-lines"

    @JvmStatic
    fun addIfNotPresent(collection: MutableList<String>, styleclass: String) {
        if (styleclass !in collection) {
            collection.add(styleclass)
        }
    }
}
