package io.github.onecdevops

import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import org.gradle.testkit.runner.TaskOutcome

class BuildFunctionalTest: FunctionalTest() {

    @Test fun `builds a cf file from a configuration`() {
        copySources("configuration", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.cf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cfe").exists())
    }

    @Test fun `builds a cfe file from an extension`() {
        copySources("extension", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.cfe").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `builds an epf file from an external data processor`() {
        copySources("data-processor", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.epf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `builds an erf file from an external report`() {
        copySources("report", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.erf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `builds from another source directory`() {
        copySources("configuration", "cf")
        buildFile.appendText("\nonec.srcDir = file('cf')")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.cf").length() > 0)
    }

    @Test fun `names the file after the archives name`() {
        copySources("configuration", "src")
        buildFile.appendText("\nbase.archivesName = 'accounting'")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/accounting.cf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `does not rebuild unchanged sources`() {
        copySources("configuration", "src")
        runner("onecBuild").build()

        val result = runner("onecBuild").build()

        assertEquals(TaskOutcome.UP_TO_DATE, result.task(":onecBuild")?.outcome)
    }

    @Test fun `skips the build without sources`() {
        val result = runner("assemble").build()

        assertEquals(TaskOutcome.NO_SOURCE, result.task(":onecBuild")?.outcome)
    }

    @Test fun `fails with the ibcmd output when the sources are invalid`() {
        projectDir.resolve("src").mkdirs()
        projectDir.resolve("src/Configuration.xml").writeText("""
            <MetaDataObject><Configuration><Properties><Name>Broken</Name></Properties></Configuration></MetaDataObject>
        """.trimIndent())

        val result = runner("onecBuild").buildAndFail()

        assertContains(result.output, "ibcmd infobase config import failed with exit code")
        assertContains(result.output, "Configuration.xml")
    }

    @Test fun `leaves no temporary files after a build`() {
        copySources("configuration", "src")

        runner("onecBuild").build()

        assertEquals(emptyList(), projectDir.resolve("build/tmp").walk().filter { it.isFile }.toList())
    }

    @Test fun `takes the result from the build cache after a clean`() {
        copySources("configuration", "src")
        settingsFile.appendText("\nbuildCache { local { directory = file('build-cache') } }")
        runner("onecBuild", "--build-cache").build()
        runner("clean").build()

        val result = runner("onecBuild", "--build-cache").build()

        assertEquals(TaskOutcome.FROM_CACHE, result.task(":onecBuild")?.outcome)
    }

    @Test fun `reuses the configuration cache when the sources change`() {
        copySources("configuration", "src")
        runner("onecBuild", "--configuration-cache").build()
        projectDir.resolve("src/Configuration.xml").appendText("<!-- changed -->")

        val result = runner("onecBuild", "--configuration-cache").build()

        assertContains(result.output, "Reusing configuration cache.")
    }
}
