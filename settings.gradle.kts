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
            library("okio", "com.squareup.okio:okio:3.9.1")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.18.0")
            library("csv", "de.siegmar:fastcsv:3.3.1")
            library("oshi", "com.github.oshi:oshi-core:6.6.4")
            library("compress", "org.apache.commons:commons-compress:1.27.1")
            library("xz", "org.tukaani:xz:1.10")
            library("xml", "com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.18.0")
            library("logback", "ch.qos.logback:logback-classic:1.5.8")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:5.9.1")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:5.9.1")
            bundle("kotest", listOf("kotest-assertions-core", "kotest-runner-junit5"))
        }
    }
}

include(":core")
include(":jmetro")
include(":desktop")
