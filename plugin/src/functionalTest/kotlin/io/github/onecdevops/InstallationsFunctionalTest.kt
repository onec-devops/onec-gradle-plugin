package io.github.onecdevops

import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.Test

class InstallationsFunctionalTest: FunctionalTest() {

    // The marked line of onecInstallations: version, home and the setting the installation comes from.
    private val selectedInstallation = Regex("""^\* 8\.3\.\S+ {3,}(\S.*?) {3,}(\S.*)$""", RegexOption.MULTILINE)

    @Test fun `marks the installation that matches the requested version`() {
        val result = runner("onecInstallations").build()

        assertNotNull(selectedInstallation.find(result.output))
    }

    @Test fun `selects an installation given in the Gradle property`() {
        val home = selectedInstallation.find(runner("onecInstallations").build().output)!!.groupValues[1]

        val result = runner("onecInstallations", "-Ponec.installations.paths=$home").build()

        assertEquals("onec.installations.paths", selectedInstallation.find(result.output)?.groupValues?.get(2))
    }
}
