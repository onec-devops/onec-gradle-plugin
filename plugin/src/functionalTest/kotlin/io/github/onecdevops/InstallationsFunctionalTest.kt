package io.github.onecdevops

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.Test

class InstallationsFunctionalTest: FunctionalTest() {

    @Test fun `marks the platform installation that matches the requested version`() {
        val result = runner("onecInstallations").build()

        assertNotNull(selectedPlatform.find(result.output))
    }

    @Test fun `marks the 1C EDT installation that matches the requested version`() {
        buildFile.appendText("\nonec.edtVersion = '$EDT_VERSION'")

        val result = runner("onecInstallations").build()

        assertNotNull(selectedEdt.find(result.output))
    }

    @Test fun `selects a platform installation given in the Gradle property`() {
        val home = selectedPlatformHome()

        val result = runner("onecInstallations", "-Ponec.installations.paths=$home").build()

        assertEquals("onec.installations.paths", selectedPlatform.find(result.output)?.groups?.get("source")?.value)
    }
}
