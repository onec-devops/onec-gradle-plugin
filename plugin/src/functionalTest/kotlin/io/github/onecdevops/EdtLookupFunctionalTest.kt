package io.github.onecdevops

import kotlin.test.assertContains
import kotlin.test.BeforeTest
import kotlin.test.Test

/** Lookup of 1C:EDT; the builds fail before they would start it, so they need neither 1C:EDT nor a real project. */
class EdtLookupFunctionalTest: FunctionalTest() {

    private val edtDir by lazy { projectDir.resolve("edt") }

    // The files the plugin tells a 1C:EDT project by.
    @BeforeTest fun createEdtProject() {
        projectDir.resolve("src/DT-INF").mkdirs()
        projectDir.resolve("src/DT-INF/PROJECT.PMF").writeText("Manifest-Version: 1.0\n")
        projectDir.resolve("src/.project").writeText("<projectDescription><name>sample</name></projectDescription>")
    }

    @Test fun `names the directory from the Gradle property when 1C EDT is not found`() {
        requestMissingEdt()

        val result = runner("onecBuild", "-Ponec.edt.installations.paths=${edtDir.path}").buildAndFail()

        assertContains(result.output, "  ${edtDir.path}  (onec.edt.installations.paths)")
    }

    @Test fun `names the directory from EDT_HOME when 1C EDT is not found`() {
        requestMissingEdt()

        val result = runner("onecBuild")
            .withEnvironment(System.getenv() + ("EDT_HOME" to edtDir.path))
            .buildAndFail()

        assertContains(result.output, "  ${edtDir.path}  (environment variable EDT_HOME)")
    }

    @Test fun `names the directory from a custom environment variable when 1C EDT is not found`() {
        requestMissingEdt()

        val result = runner("onecBuild", "-Ponec.edt.installations.fromEnv=EDT_DIR")
            .withEnvironment(System.getenv() + ("EDT_DIR" to edtDir.path))
            .buildAndFail()

        assertContains(result.output, "  ${edtDir.path}  (environment variable EDT_DIR)")
    }

    @Test fun `names the requested version when 1C EDT is not found`() {
        requestMissingEdt()

        val result = runner("onecBuild").buildAndFail()

        assertContains(result.output, "1C:EDT 0.0 with 1cedtcli not found")
    }

    @Test fun `asks for the 1C EDT version when it is not set`() {
        val result = runner("onecBuild").buildAndFail()

        assertContains(result.output, "onec.edtVersion is not set")
    }

    private fun requestMissingEdt() {
        buildFile.appendText("\nonec.edtVersion = '0.0'")
    }
}
