package io.github.onecdevops

import kotlin.test.assertContains
import kotlin.test.Test

class PlatformLookupFunctionalTest: FunctionalTest() {

    private val platformDir by lazy { projectDir.resolve("platform") }

    @Test fun `names the directory from the Gradle property when the platform is not found`() {
        requestMissingPlatform()

        val result = runner("onecBuild", "-Ponec.installations.paths=${platformDir.path}").buildAndFail()

        assertContains(result.output, "  ${platformDir.path}  (onec.installations.paths)")
    }

    @Test fun `names the directory from ONEC_HOME when the platform is not found`() {
        requestMissingPlatform()

        val result = runner("onecBuild")
            .withEnvironment(System.getenv() + ("ONEC_HOME" to platformDir.path))
            .buildAndFail()

        assertContains(result.output, "  ${platformDir.path}  (environment variable ONEC_HOME)")
    }

    @Test fun `names the directory from a custom environment variable when the platform is not found`() {
        requestMissingPlatform()

        val result = runner("onecBuild", "-Ponec.installations.fromEnv=PLATFORM_DIR")
            .withEnvironment(System.getenv() + ("PLATFORM_DIR" to platformDir.path))
            .buildAndFail()

        assertContains(result.output, "  ${platformDir.path}  (environment variable PLATFORM_DIR)")
    }

    @Test fun `names the requested version when the platform is not found`() {
        requestMissingPlatform()

        val result = runner("onecBuild").buildAndFail()

        assertContains(result.output, "1C:Enterprise platform 0.0 with ibcmd not found")
    }

    private fun requestMissingPlatform() {
        copySources("configuration", "src")
        buildFile.appendText("\nonec.platformVersion = '0.0'")
    }
}
