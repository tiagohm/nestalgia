import org.springframework.boot.gradle.tasks.bundling.BootJar
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

plugins {
    kotlin("jvm")
    kotlin("kapt")
    id("maven-publish")
    id("com.github.gmazzo.buildconfig")
    id("org.springframework.boot") version "3.2.2"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("plugin.spring")
    id("org.openjfx.javafxplugin")
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
    implementation("com.badlogicgames.gdx:gdx:1.12.1") // 1.9.9
    implementation("com.badlogicgames.gdx:gdx-controllers:1.9.13") // 1.9.9
    implementation("com.badlogicgames.gdx:gdx-jnigen:2.5.1") // 1.9.10

    implementation("org.springframework.boot:spring-boot-starter")
    kapt("org.springframework:spring-context-indexer:6.1.4")
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

tasks.withType<BootJar> {
    archiveFileName.set("nestalgia.jar")

    isZip64 = true

    manifest {
        attributes["Start-Class"] = "br.tiagohm.nestalgia.desktop.MainKt"
    }
}
