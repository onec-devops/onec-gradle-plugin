package io.github.onecdevops

import org.gradle.api.Project
import org.gradle.api.Plugin

class OnecGradlePluginPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'io.github.onec-devops.greeting'")
            }
        }
    }
}
