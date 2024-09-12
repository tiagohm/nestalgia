package br.tiagohm.nestalgia.desktop.app

import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.scheduling.annotation.EnableAsync
import java.nio.file.Path
import kotlin.io.path.createDirectories

@EnableAsync
@SpringBootApplication
@ComponentScan(basePackages = ["br.tiagohm.nestalgia.desktop"])
class App : CommandLineRunner {

    @Bean
    fun appDir(): Path = Path.of(System.getProperty("app.dir"))

    @Bean
    fun screenshotDir(appDir: Path) = Path.of("$appDir", "screenshots").createDirectories()

    @Bean
    fun saveDir(appDir: Path) = Path.of("$appDir", "saves").createDirectories()

    override fun run(vararg args: String?) = Unit
}
