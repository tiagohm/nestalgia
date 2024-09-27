plugins {
    kotlin("jvm")
    id("maven-publish")
}

dependencies {
    implementation(libs.compress)
    implementation(libs.xz)
    implementation(libs.logback)
    testImplementation(libs.xml)
    testImplementation(libs.sqlite)
    testImplementation(libs.bundles.exposed)
    testImplementation(libs.bundles.kotest)
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            from(components["java"])
        }
    }
}
