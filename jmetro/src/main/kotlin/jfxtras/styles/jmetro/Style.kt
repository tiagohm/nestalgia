package jfxtras.styles.jmetro

enum class Style(resourceName: String) {
    LIGHT("light-theme.css"),
    DARK("dark-theme.css");

    val styleStylesheetURL = Thread.currentThread().contextClassLoader
        .getResource(resourceName)!!
        .toExternalForm()!!
}
