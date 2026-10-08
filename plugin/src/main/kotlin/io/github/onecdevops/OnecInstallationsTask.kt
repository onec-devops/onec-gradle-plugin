package io.github.onecdevops

import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.UntrackedTask

/** Prints the platform installations the plugin finds and marks the one it uses. */
@UntrackedTask(because = "Reports what is installed on the machine")
abstract class OnecInstallationsTask: PlatformTask() {
    @TaskAction
    fun list() {
        val locations = installationLocations()
        val installations = PlatformInstallation.discover(locations)
        val requested = platformVersion.orNull
        val selected = requested?.let { PlatformInstallation.select(it, installations) }

        val lines = mutableListOf("Requested: ${requested ?: "onec.platformVersion is not set"}", "")
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
        logger.lifecycle(lines.joinToString("\n"))
    }
}
