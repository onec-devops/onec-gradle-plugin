package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.util.GradleVersion

class OnecGradlePluginPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        requireSupportedGradle(GradleVersion.current())
        project.tasks.register("greeting") { task ->
            task.doLast {
                println("Hello from plugin 'io.github.onec-devops.greeting'")
            }
        }
    }

    companion object {
        const val MINIMUM_GRADLE_VERSION = "9.0.0"
    }
}

internal fun requireSupportedGradle(current: GradleVersion) {
    if (current.baseVersion < GradleVersion.version(OnecGradlePluginPlugin.MINIMUM_GRADLE_VERSION)) {
        throw GradleException("The onec plugin requires Gradle ${OnecGradlePluginPlugin.MINIMUM_GRADLE_VERSION} or newer, " +
            "this was ${current.version}")
    }
}
