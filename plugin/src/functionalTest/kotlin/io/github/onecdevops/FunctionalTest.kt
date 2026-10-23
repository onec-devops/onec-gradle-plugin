package io.github.onecdevops

import java.io.File
import kotlin.test.BeforeTest
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

/**
 * A test that runs builds of a project with the plugin applied. The builds call the real ibcmd
 * and fail without an installed platform; a machine that keeps it outside the standard
 * directories points ONEC_HOME to it.
 */
abstract class FunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    protected val buildFile by lazy { projectDir.resolve("build.gradle") }
    protected val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @BeforeTest fun createProject() {
        settingsFile.writeText("rootProject.name = 'sample'")
        buildFile.writeText("""
            plugins {
                id('io.github.onec-devops.onec')
            }

            onec {
                platformVersion = '$PLATFORM_VERSION'
            }
        """.trimIndent())
    }

    protected fun runner(vararg arguments: String): GradleRunner {
        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments(*arguments)
        runner.withProjectDir(projectDir)
        System.getProperty("testGradleVersion")?.let { runner.withGradleVersion(it) }
        return runner
    }

    protected fun copySources(resource: String, target: String) {
        File(javaClass.getResource("/$resource")!!.toURI()).copyRecursively(projectDir.resolve(target))
    }

    /** Home of the platform installation that onecInstallations marks. */
    protected fun selectedPlatformHome(): String =
        selectedPlatform.find(runner("onecInstallations").build().output)!!.groups["home"]!!.value

    /** Loads the configuration [file] into a new infobase and exports it to XML, to see what was built. */
    protected fun exportToXml(file: File): File {
        val home = File(selectedPlatformHome())
        val ibcmd = listOf("bin/ibcmd.exe", "ibcmd").map { home.resolve(it) }.first { it.isFile }
        val work = projectDir.resolve("export")
        val infobase = arrayOf("--data=${work.resolve("data")}", "--db-path=${work.resolve("ib")}")
        for (args in listOf(
            arrayOf("infobase", "create", *infobase),
            arrayOf("infobase", "config", "load", *infobase, file.path),
            arrayOf("infobase", "config", "export", *infobase, work.resolve("xml").path))) {
            val process = ProcessBuilder(ibcmd.path, *args).redirectErrorStream(true).start()
            val output = process.inputStream.readBytes().decodeToString()
            check(process.waitFor() == 0) { "ibcmd ${args.take(3).joinToString(" ")} failed:\n$output" }
        }
        return work.resolve("xml")
    }

    companion object {
        /** The platform the tests ask for: `-PtestPlatformVersion`, by default any build of 8.3. */
        val PLATFORM_VERSION: String = System.getProperty("testPlatformVersion") ?: "8.3"

        /** The 1C:EDT the tests that need it ask for: `-PtestEdtVersion`, by default any build of 2026. */
        val EDT_VERSION: String = System.getProperty("testEdtVersion") ?: "2026"

        // The marked platform and 1C:EDT lines of onecInstallations.
        val selectedPlatform = selectedInstallation(PLATFORM_VERSION)
        val selectedEdt = selectedInstallation(EDT_VERSION)

        /** The line of onecInstallations marking the installation of [requested]: its version, home and the setting it comes from. */
        private fun selectedInstallation(requested: String) = Regex(
            """^\* (?<version>${Regex.escape(requested)}(?:\.\S+)?) {3,}(?<home>\S.*?) {3,}(?<source>\S.*)$""",
            RegexOption.MULTILINE)
    }
}
