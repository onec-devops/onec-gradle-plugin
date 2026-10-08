package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.util.GradleVersion
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class OnecGradlePluginPluginTest {
    @Test fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.onec-devops.greeting")

        assertNotNull(project.tasks.findByName("greeting"))
    }

    @ParameterizedTest
    @ValueSource(strings = [OnecGradlePluginPlugin.MINIMUM_GRADLE_VERSION, "${OnecGradlePluginPlugin.MINIMUM_GRADLE_VERSION}-rc-1", "9.8.0"])
    fun `accepts Gradle of the minimum version or newer`(version: String) {
        val current = GradleVersion.version(version)

        assertDoesNotThrow { requireSupportedGradle(current) }
    }

    @Test fun `rejects Gradle older than the minimum version`() {
        val current = GradleVersion.version("8.14.3")

        val failure = assertFailsWith<GradleException> { requireSupportedGradle(current) }

        assertEquals("The onec plugin requires Gradle ${OnecGradlePluginPlugin.MINIMUM_GRADLE_VERSION} or newer, this was 8.14.3", failure.message)
    }
}
