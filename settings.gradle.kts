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
            library("okio", "com.squareup.okio:okio:3.9.0")
            library("jackson", "com.fasterxml.jackson.core:jackson-databind:2.17.1")
            library("csv", "de.siegmar:fastcsv:3.1.0")
            library("oshi", "com.github.oshi:oshi-core:6.6.1")
            library("logback", "ch.qos.logback:logback-classic:1.5.6")
            library("kotest-assertions-core", "io.kotest:kotest-assertions-core:5.9.0")
            library("kotest-runner-junit5", "io.kotest:kotest-runner-junit5:5.9.0")
            bundle("kotest", listOf("kotest-assertions-core", "kotest-runner-junit5"))
        }
    }
}

include(":core")
include(":jmetro")
include(":desktop")
