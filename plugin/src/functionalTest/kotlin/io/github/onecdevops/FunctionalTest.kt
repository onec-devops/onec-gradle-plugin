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
                platformVersion = '8.3'
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
}
