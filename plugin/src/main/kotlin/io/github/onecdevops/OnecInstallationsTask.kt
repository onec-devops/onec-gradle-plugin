package io.github.onecdevops

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** Prints the platform and 1C:EDT installations the plugin finds and marks the ones it uses. */
@UntrackedTask(because = "Reports what is installed on the machine")
abstract class OnecInstallationsTask: PlatformTask(), UsesEdt {
    @TaskAction
    fun list() {
        val platform = report("1C:Enterprise platform", "onec.platformVersion", platformVersion.orNull,
            PlatformInstallations, installationLocations())
        val edt = report("1C:EDT", "onec.edtVersion", edtVersion.orNull,
            EdtInstallations, edtInstallationLocations(windowsProgramDirs.get()))
        logger.lifecycle((platform + "" + edt).joinToString("\n"))
    }

    private fun report(
        title: String,
        setting: String,
        requested: String?,
        kind: InstallationKind,
        locations: List<InstallationLocation>
    ): List<String> {
        val installations = kind.discover(locations)
        val selected = requested?.let { kind.select(it, installations) }

        val lines = mutableListOf("$title, requested: ${requested ?: "$setting is not set"}", "")
        if (installations.isEmpty()) {
            lines += "No installations found, looked in:"
            lines += locations.map { "  ${it.path}  (${it.source})" }
        } else {
            val versionWidth = installations.maxOf { it.version.length }
            val homeWidth = installations.maxOf { it.home.path.length }
            lines += installations.map {
                val mark = if (it === selected) "*" else " "
                "$mark ${it.version.padEnd(versionWidth)}   ${it.home.path.padEnd(homeWidth)}   ${it.source}"
            }
        }
        return lines
    }
}
