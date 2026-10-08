package io.github.onecdevops

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class OnecGradlePluginPluginTest {
    @Test fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("io.github.onec-devops.greeting")

        assertNotNull(project.tasks.findByName("greeting"))
    }
}
