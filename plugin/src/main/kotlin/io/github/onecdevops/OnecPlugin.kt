package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.util.GradleVersion

class OnecPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        requireSupportedGradle(GradleVersion.current())
        val onec = project.extensions.create("onec", OnecExtension::class.java)

        val providers = project.providers
        val installationPaths = providers.gradleProperty(PlatformTask.INSTALLATION_PATHS_PROPERTY).orElse("")
            .map { PlatformInstallation.parseList(it) }
        val installationVariables = providers.gradleProperty(PlatformTask.INSTALLATION_VARIABLES_PROPERTY)
            .orElse(PlatformTask.DEFAULT_INSTALLATION_VARIABLE)
            .map { PlatformInstallation.parseList(it) }
        project.tasks.withType(PlatformTask::class.java).configureEach { task ->
            task.platformVersion.set(onec.platformVersion)
            task.installationPaths.set(installationPaths)
            task.installationVariables.set(installationVariables)
            task.windowsProgramDirs.add(providers.environmentVariable("ProgramFiles").orElse("C:\\Program Files"))
            task.windowsProgramDirs.add(providers.environmentVariable("ProgramFiles(x86)").orElse("C:\\Program Files (x86)"))
        }
        project.tasks.register("onecInstallations", OnecInstallationsTask::class.java) { task ->
            task.group = "help"
            task.description = "Displays the detected 1C:Enterprise platform installations."
        }
    }

    companion object {
        const val MINIMUM_GRADLE_VERSION = "9.0.0"
    }
}

internal fun requireSupportedGradle(current: GradleVersion) {
    if (current.baseVersion < GradleVersion.version(OnecPlugin.MINIMUM_GRADLE_VERSION)) {
        throw GradleException("The onec plugin requires Gradle ${OnecPlugin.MINIMUM_GRADLE_VERSION} or newer, " +
            "this was ${current.version}")
    }
}
