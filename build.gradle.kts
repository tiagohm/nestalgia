import com.adarshr.gradle.testlogger.TestLoggerExtension
import com.adarshr.gradle.testlogger.theme.ThemeType
import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
        classpath("com.adarshr:gradle-test-logger-plugin:4.0.0")
        classpath("com.github.gmazzo:gradle-buildconfig-plugin:3.1.0")
        classpath("org.jetbrains.kotlin:kotlin-allopen:1.9.22")
        classpath("org.openjfx:javafx-plugin:0.1.0")
    }

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

allprojects {
    group = "com.github.tiagohm"
    version = project.properties["version.code"]!!.toString()

    repositories {
        mavenCentral()
        google()
        // jcenter()
        maven { setUrl("https://jitpack.io") }
    }

    tasks.create("downloadDependencies") {
        description = "Download all dependencies to the Gradle cache"
        doLast {
            for (configuration in configurations) {
                if (configuration.isCanBeResolved) {
                    configuration.files
                }
            }
        }
    }
}

subprojects {
    val project = this@subprojects
    if (project.name == "nestalgia-bom") return@subprojects

    apply {
        plugin("com.adarshr.test-logger")
    }

    configure<TestLoggerExtension> {
        theme = ThemeType.STANDARD
        slowThreshold = 2000L
        showStackTraces = false
        showSkipped = true
        showStandardStreams = true
        showPassedStandardStreams = true
        showSkippedStandardStreams = false
        showFailedStandardStreams = true
        logLevel = LogLevel.QUIET
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()
        kotlinOptions.freeCompilerArgs = listOf(
            "-Xjvm-default=all", "-Xjsr305=strict",
            "-Xno-param-assertions", "-Xno-call-assertions", "-Xno-receiver-assertions",
            "-opt-in=kotlin.io.path.ExperimentalPathApi",
        )
    }

    tasks.withType<Test> {
        useJUnitPlatform()

        maxParallelForks = Runtime.getRuntime().availableProcessors()
        reports.html.required.set(false)
        reports.junitXml.required.set(false)

        testLogging {
            exceptionFormat = TestExceptionFormat.FULL
        }

        systemProperty("junit.jupiter.extensions.autodetection.enabled", "true")
        systemProperty("github", System.getProperty("github", "false"))
    }

    tasks.withType<JavaCompile> {
        options.isFork = true
        options.encoding = Charsets.UTF_8.toString()
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
}

tasks.wrapper {
    distributionType = Wrapper.DistributionType.ALL
}
