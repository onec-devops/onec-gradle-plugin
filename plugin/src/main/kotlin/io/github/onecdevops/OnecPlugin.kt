package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Plugin
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.util.GradleVersion

class OnecPlugin: Plugin<Project> {
    override fun apply(project: Project) {
        requireSupportedGradle(GradleVersion.current())
        project.pluginManager.apply("base")

        val onec = project.extensions.create("onec", OnecExtension::class.java)

        val providers = project.providers
        val installationPaths = providers.gradleProperty(PlatformTask.INSTALLATION_PATHS_PROPERTY).orElse("")
            .map { parseList(it) }
        val installationVariables = providers.gradleProperty(PlatformTask.INSTALLATION_VARIABLES_PROPERTY)
            .orElse(PlatformTask.DEFAULT_INSTALLATION_VARIABLE)
            .map { parseList(it) }
        val edtInstallationPaths = providers.gradleProperty(UsesEdt.INSTALLATION_PATHS_PROPERTY).orElse("")
            .map { parseList(it) }
        val edtInstallationVariables = providers.gradleProperty(UsesEdt.INSTALLATION_VARIABLES_PROPERTY)
            .orElse(UsesEdt.DEFAULT_INSTALLATION_VARIABLE)
            .map { parseList(it) }
        project.tasks.withType(PlatformTask::class.java).configureEach { task ->
            task.platformVersion.set(onec.platformVersion)
            task.installationPaths.set(installationPaths)
            task.installationVariables.set(installationVariables)
            task.windowsProgramDirs.add(providers.environmentVariable("ProgramFiles").orElse("C:\\Program Files"))
            task.windowsProgramDirs.add(providers.environmentVariable("ProgramFiles(x86)").orElse("C:\\Program Files (x86)"))
        }
        project.tasks.withType(UsesEdt::class.java).configureEach { task ->
            task.edtVersion.set(onec.edtVersion)
            task.edtInstallationPaths.set(edtInstallationPaths)
            task.edtInstallationVariables.set(edtInstallationVariables)
            task.localAppData.set(providers.environmentVariable("LOCALAPPDATA").orElse(""))
        }
        project.tasks.register("onecInstallations", OnecInstallationsTask::class.java) { task ->
            task.group = "help"
            task.description = "Displays the detected 1C:Enterprise platform and 1C:EDT installations."
        }

        val outputDirectory = project.layout.buildDirectory.dir("onec")
        val infobase = project.tasks.register("onecCreateInfobase", OnecCreateInfobaseTask::class.java) { task ->
            task.description = "Creates the infobase used as a template by the builds."
            task.infobaseDir.set(outputDirectory.map { it.dir("infobase") })
        }
        project.tasks.withType(OnecBuildTask::class.java).configureEach { task ->
            task.group = "build"
            task.infobaseDir.set(infobase.flatMap { it.infobaseDir })
            task.dependsOn(infobase)
            task.outputDirectory.set(outputDirectory)
        }

        // A project builds one file; like a jar, it is named after base.archivesName, the project name by default.
        onec.srcDir.convention(project.layout.projectDirectory.dir("src"))
        val archivesName = project.extensions.getByType(BasePluginExtension::class.java).archivesName
        val build = project.tasks.register("onecBuild", OnecBuildTask::class.java) { task ->
            task.description = "Builds the project into a .cf, .cfe, .epf or .erf file."
            task.srcDir.set(onec.srcDir)
            task.baseName.set(archivesName)
        }
        project.tasks.named("assemble") { it.dependsOn(build) }
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
