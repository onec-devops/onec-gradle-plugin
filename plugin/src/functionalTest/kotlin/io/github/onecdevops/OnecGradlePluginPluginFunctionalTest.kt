package io.github.onecdevops

import java.io.File
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.GradleRunner
import org.junit.jupiter.api.io.TempDir

class OnecGradlePluginPluginFunctionalTest {

    @field:TempDir
    lateinit var projectDir: File

    private val buildFile by lazy { projectDir.resolve("build.gradle") }
    private val settingsFile by lazy { projectDir.resolve("settings.gradle") }

    @Test fun `can run task`() {
        settingsFile.writeText("")
        buildFile.writeText("""
            plugins {
                id('io.github.onec-devops.greeting')
            }
        """.trimIndent())

        val runner = GradleRunner.create()
        runner.forwardOutput()
        runner.withPluginClasspath()
        runner.withArguments("greeting")
        runner.withProjectDir(projectDir)
        System.getProperty("testGradleVersion")?.let { runner.withGradleVersion(it) }
        val result = runner.build()

        assertTrue(result.output.contains("Hello from plugin 'io.github.onec-devops.greeting'"))
    }
}
