package org.example

import org.gradle.api.Project
import org.gradle.api.Plugin

class OnecGradlePluginPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'org.example.greeting'")
            }
        }
    }
}
