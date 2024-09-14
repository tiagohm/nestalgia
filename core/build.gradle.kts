plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    implementation(libs.logback)
    testImplementation(libs.bundles.kotest)
    testImplementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.17.1")
    testImplementation("com.fasterxml.woodstox:woodstox-core:7.0.0")
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            from(components["java"])
        }
    }
}
