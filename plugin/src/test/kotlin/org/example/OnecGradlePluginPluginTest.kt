package org.example

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class OnecGradlePluginPluginTest {
    @Test fun `plugin registers task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.example.greeting")

        assertNotNull(project.tasks.findByName("greeting"))
    }
}
