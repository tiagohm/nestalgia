rootProject.name = "nestalgia"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

buildCache {
    local {
        directory = File(rootDir, ".cache")
        removeUnusedEntriesAfterDays = 30
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("okio", "com.squareup.okio:okio:3.17.0")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.21.3")
            library("csv", "de.siegmar:fastcsv:4.2.0")
            library("oshi", "com.github.oshi:oshi-core:7.0.0")
            library("compress", "org.apache.commons:commons-compress:1.28.0")
            library("xz", "org.tukaani:xz:1.12")
            library("xml", "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.21.3")
            library("exposed-core", "org.jetbrains.exposed:exposed-core:1.2.0")
            library("exposed-jdbc", "org.jetbrains.exposed:exposed-jdbc:1.2.0")
            library("sqlite", "org.xerial:sqlite-jdbc:3.53.0.0")
            library("logback", "ch.qos.logback:logback-classic:1.5.32")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:6.1.11")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:6.1.11")
            bundle("kotest", listOf("kotest-assertions-core", "kotest-runner-junit5"))
            bundle("exposed", listOf("exposed-core", "exposed-jdbc"))
        }
    }
}

include(":core")
include(":jmetro")
include(":desktop")
