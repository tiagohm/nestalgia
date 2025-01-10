import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm")
    id("maven-publish")
    id("com.github.gmazzo.buildconfig")
    id("org.openjfx.javafxplugin")
    id("com.gradleup.shadow") version "8.3.5"
}

dependencies {
    implementation(project(":core"))
    implementation(project(":jmetro"))
    implementation(libs.oshi)

    implementation("com.github.WilliamAHartman:Jamepad:1.4.0")
    // JCenter deprecation.
    // implementation("uk.co.electronstudio.sdl2gdx:sdl2gdx:1.0.4")
    implementation(files("$projectDir/libs/sdl2gdx-1.0.4.jar"))
    // sdl2gdx's dependencies.
    implementation("com.badlogicgames.gdx:gdx:1.13.1") // 1.9.9
    implementation("com.badlogicgames.gdx:gdx-controllers:1.9.13") // 1.9.9
    implementation("com.badlogicgames.gdx:gdx-jnigen:2.5.2") // 1.9.10
    implementation("com.github.kwhat:jnativehook:2.2.2")

    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation(libs.logback)
    testImplementation(libs.bundles.kotest)
}

publishing {
    publications {
        create<MavenPublication>("pluginMaven") {
            from(components["java"])
        }
    }
}

buildConfig {
    packageName("br.tiagohm.nestalgia.desktop")
    useKotlinOutput()
    buildConfigField("String", "VERSION_CODE", "\"${project.properties["version.code"]}\"")
    buildConfigField("String", "BUILD_DATE", "\"${LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))}\"")
    buildConfigField("String", "BUILD_TIME", "\"${LocalTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("HH:mm"))}\"")
}

javafx {
    version = properties["javaFX.version"]!!.toString()
    modules = listOf("javafx.controls", "javafx.fxml")
}

tasks.withType<ShadowJar> {
    archiveFileName.set("nestalgia.jar")

    isZip64 = true
    minimize {
        exclude(dependency("org.openjfx:.*:.*"))
    }

    manifest {
        attributes["Main-Class"] = "br.tiagohm.nestalgia.desktop.MainKt"
    }
}
