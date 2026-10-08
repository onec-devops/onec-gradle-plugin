package io.github.onecdevops

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal
import org.gradle.work.DisableCachingByDefault
import java.io.File

/** A task that needs to find the installed platform. */
@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class PlatformTask: DefaultTask() {
    /** Full platform version or its leading part, e.g. `8.3.25`. */
    @get:Internal
    abstract val platformVersion: Property<String>

    /** Installations, or directories with them, from the Gradle property; searched first. */
    @get:Internal
    abstract val installationPaths: ListProperty<String>

    /** Names of the environment variables that point to installations. */
    @get:Internal
    abstract val installationVariables: ListProperty<String>

    @get:Internal
    abstract val windowsProgramDirs: ListProperty<String>

    internal fun installationLocations(): List<InstallationLocation> =
        installationPaths.get().map { InstallationLocation(File(it), INSTALLATION_PATHS_PROPERTY) } +
            installationVariables.get().mapNotNull { name ->
                System.getenv(name)?.takeIf { it.isNotBlank() }
                    ?.let { InstallationLocation(File(it), "environment variable $name") }
            } +
            PlatformInstallation.standardLocations(windowsProgramDirs.get())

    companion object {
        const val INSTALLATION_PATHS_PROPERTY = "onec.installations.paths"
        const val INSTALLATION_VARIABLES_PROPERTY = "onec.installations.fromEnv"
        const val DEFAULT_INSTALLATION_VARIABLE = "ONEC_HOME"
    }
}
