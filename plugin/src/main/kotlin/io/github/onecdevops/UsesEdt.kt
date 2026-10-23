package io.github.onecdevops

import org.gradle.api.Task
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Internal

/** A task that needs to find the installed 1C:EDT. */
interface UsesEdt: Task {
    /** Full 1C:EDT version or its leading part, e.g. `2026.1`. */
    @get:Internal
    val edtVersion: Property<String>

    /** Installations, or directories with them, from the Gradle property; searched first. */
    @get:Internal
    val edtInstallationPaths: ListProperty<String>

    /** Names of the environment variables that point to installations. */
    @get:Internal
    val edtInstallationVariables: ListProperty<String>

    @get:Internal
    val localAppData: Property<String>

    companion object {
        const val INSTALLATION_PATHS_PROPERTY = "onec.edt.installations.paths"
        const val INSTALLATION_VARIABLES_PROPERTY = "onec.edt.installations.fromEnv"
        const val DEFAULT_INSTALLATION_VARIABLE = "EDT_HOME"
    }
}

internal fun UsesEdt.edtInstallationLocations(windowsProgramDirs: List<String>): List<InstallationLocation> =
    locationsFrom(edtInstallationPaths.get(), UsesEdt.INSTALLATION_PATHS_PROPERTY, edtInstallationVariables.get(),
        EdtInstallations.standardLocations(localAppData.get(), windowsProgramDirs))
