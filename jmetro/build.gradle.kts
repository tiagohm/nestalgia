plugins {
    kotlin("jvm")
    id("maven-publish")
    id("org.openjfx.javafxplugin")
}

dependencies {
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            from(components["java"])
        }
    }
}

javafx {
    version = properties["javaFX.version"]!!.toString()
    modules = listOf("javafx.controls")
}
