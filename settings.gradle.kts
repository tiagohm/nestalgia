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
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.22.1")
            library("csv", "de.siegmar:fastcsv:4.3.1")
            library("oshi", "com.github.oshi:oshi-core:7.3.2")
            library("compress", "org.apache.commons:commons-compress:1.28.0")
            library("xz", "org.tukaani:xz:1.12")
            library("xml", "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.22.1")
            library("exposed-core", "org.jetbrains.exposed:exposed-core:0.61.0")
            library("exposed-jdbc", "org.jetbrains.exposed:exposed-jdbc:0.61.0")
            library("sqlite", "org.xerial:sqlite-jdbc:3.53.2.0")
            library("logback", "ch.qos.logback:logback-classic:1.5.37")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:6.2.3")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:6.2.3")
            bundle("kotest", listOf("kotest-assertions-core", "kotest-runner-junit5"))
            bundle("exposed", listOf("exposed-core", "exposed-jdbc"))
        }
    }
}

include(":core")
include(":jmetro")
include(":desktop")
