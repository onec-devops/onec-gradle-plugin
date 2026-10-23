package io.github.onecdevops

import java.io.File
import java.nio.file.Files
import java.util.concurrent.TimeUnit
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.BeforeTest
import kotlin.test.Test

/**
 * Builds of 1C:EDT projects; they call the real 1cedtcli, which a machine without 1C:EDT Start finds through EDT_HOME.
 * The projects are made from the XML fixtures for the version of the platform in use, see [copyEdtProject].
 */
class EdtBuildFunctionalTest: FunctionalTest() {

    @BeforeTest fun requestEdt() {
        buildFile.appendText("\nonec.edtVersion = '$EDT_VERSION'")
    }

    @Test fun `builds a cf file from a 1C EDT configuration project`() {
        copyEdtProject("configuration", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.cf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cfe").exists())
    }

    @Test fun `builds a cfe file from a 1C EDT extension project`() {
        copyEdtProject("extension", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.cfe").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `builds an epf file from a 1C EDT project of an external data processor`() {
        copyEdtProject("data-processor", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.epf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `builds an erf file from a 1C EDT project of an external report`() {
        copyEdtProject("report", "src")

        runner("assemble").build()

        assertTrue(projectDir.resolve("build/onec/sample.erf").length() > 0)
        assertFalse(projectDir.resolve("build/onec/sample.cf").exists())
    }

    @Test fun `fails when a 1C EDT project of external objects holds more than one`() {
        copyEdtProject("data-processor", "src")
        copyEdtProject("report/src/ExternalReports", "src/src/ExternalReports")

        val result = runner("onecBuild").buildAndFail()

        assertContains(result.output, "must hold one external data processor or report to build, " +
            "it holds 2: ExternalDataProcessors/DataProcessor, ExternalReports/Report")
    }

    @Test fun `keeps the modules of a 1C EDT project`() {
        copyEdtProject("configuration", "src")

        runner("onecBuild").build()

        val module = exportToXml(projectDir.resolve("build/onec/sample.cf")).resolve("Ext/ManagedApplicationModule.bsl")
        assertContains(module.readText(), "Message(\"Hello\");")
    }

    @Test fun `leaves no temporary files after a build of a 1C EDT project`() {
        copyEdtProject("configuration", "src")

        runner("onecBuild").build()

        assertEquals(emptyList(), projectDir.resolve("build/tmp").walk().filter { it.isFile }.toList())
    }

    // Not a broken Configuration.mdo: 1C:EDT waits 25 minutes for such a project to start before it gives up.
    @Test fun `fails with the 1cedtcli output when the project cannot be exported`() {
        copyEdtProject("configuration", "src")
        val description = projectDir.resolve("src/.project")
        description.writeText(description.readText().replace("<name>configuration</name>", "<name>bad/name</name>"))

        val result = runner("onecBuild").buildAndFail()

        // 1cedtcli exits with 204 on Windows and with 0 on Linux, where the plugin sees that nothing was exported.
        assertContains(result.output, Regex("1cedtcli export (failed with exit code 204|exported nothing):"))
        assertContains(result.output, "Path for project must have only one segment")
    }

    /** Copies [path] of the 1C:EDT projects made from the XML fixtures, e.g. `report`, to [target] in the test project. */
    private fun copyEdtProject(path: String, target: String) {
        edtProjects(this).resolve(path).copyRecursively(projectDir.resolve(target))
    }

    /**
     * Makes a 1C:EDT project from each XML fixture, named after it. 1C:EDT does not start a project
     * for a platform version it does not support but waits for it, so the projects are made for the
     * version of the platform in use, which the 1C:EDT of the onec-docker image supports, and not
     * for the version of the fixtures. Importing them takes half a minute, so it is done once.
     */
    private fun makeEdtProjects(): File {
        val installations = runner("onecInstallations").build().output
        // 1C:EDT takes the version of the platform without the build, e.g. 8.3.25.
        val platformVersion = selectedPlatform.find(installations)!!.groups["version"]!!.value.split('.').take(3).joinToString(".")
        val edtHome = File(selectedEdt.find(installations)!!.groups["home"]!!.value)
        val edtcli = listOf("1cedtcli.exe", "1cedtcli").map { edtHome.resolve(it) }.first { it.isFile }

        val dir = Files.createTempDirectory("edt-projects").toFile()
        Runtime.getRuntime().addShutdownHook(Thread { dir.deleteRecursively() })
        val projects = listOf("configuration", "extension", "data-processor", "report")
        val script = dir.resolve("import.txt")
        script.writeText(projects.joinToString("\n") { name ->
            val xml = File(javaClass.getResource("/$name")!!.toURI()).invariantSeparatorsPath
            val base = if (name == "extension") " --base-project-name configuration" else ""
            "import --version $platformVersion$base --configuration-files \"$xml\" " +
                "--project \"${dir.resolve(name).invariantSeparatorsPath}\""
        })
        val process = ProcessBuilder(edtcli.path, "-data", dir.resolve("workspace").path,
            "-vmargs", "-Dfile.encoding=UTF-8", "-file", script.path)
            .redirectErrorStream(true).redirectOutput(dir.resolve("import.log")).start()
        if (!process.waitFor(10, TimeUnit.MINUTES)) {
            process.destroyForcibly()
        }
        // 1cedtcli exits with 0 on most errors, so the result is told by the projects.
        check(projects.all { dir.resolve("$it/DT-INF/PROJECT.PMF").isFile }) {
            "1cedtcli did not make the 1C:EDT projects for platform $platformVersion:\n" +
                dir.resolve("import.log").readText() +
                dir.resolve("workspace/.metadata/.log").takeIf { it.isFile }?.readText().orEmpty()
        }
        return dir
    }

    companion object {
        private var edtProjects: Result<File>? = null

        // The tests run in parallel; the first one makes the projects, and a failure fails all at once.
        @Synchronized
        private fun edtProjects(test: EdtBuildFunctionalTest): File =
            (edtProjects ?: runCatching { test.makeEdtProjects() }.also { edtProjects = it }).getOrThrow()
    }
}
