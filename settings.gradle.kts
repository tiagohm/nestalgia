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
            library("okio", "com.squareup.okio:okio:3.6.0")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.15.3")
            library("csv", "de.siegmar:fastcsv:2.2.2")
            library("oshi", "com.github.oshi:oshi-core:6.4.7")
            library("logback", "ch.qos.logback:logback-classic:1.4.11")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:5.8.0")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:5.7.2")
            bundle("kotest", listOf("kotest-assertions-core", "kotest-runner-junit5"))
        }
    }
}

include(":core")
include(":jmetro")
include(":desktop")
