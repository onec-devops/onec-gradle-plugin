package io.github.onecdevops

import java.io.File

/** A place to look for installations, with the setting it comes from. */
internal class InstallationLocation(val path: File, val source: String)

internal class PlatformInstallation(val version: String, val home: File, val ibcmd: File, val source: String) {
    companion object {
        private val versionPattern = Regex("""\d+(\.\d+)*""")
        private val ibcmdPaths = listOf("bin/ibcmd.exe", "ibcmd")

        /**
         * Lists the installations found in [locations], in their order. A location is either
         * one installation or a directory with an installation per version.
         */
        fun discover(locations: List<InstallationLocation>): List<PlatformInstallation> =
            locations.flatMap { location ->
                val single = installation(location.path, location.source)
                if (single != null || ibcmdIn(location.path) != null) {
                    listOfNotNull(single)
                } else {
                    location.path.listFiles { file -> file.isDirectory }.orEmpty().sorted()
                        .mapNotNull { installation(it, location.source) }
                }
            }.distinctBy { it.ibcmd.toPath().toRealPath() }

        /**
         * Picks the newest of [installations] matching [requested], which is either a full version
         * (`8.3.25.1633`) or its leading part (`8.3.25`); of equal versions the earlier one wins.
         */
        fun select(requested: String, installations: List<PlatformInstallation>): PlatformInstallation? =
            installations
                .filter { it.version == requested || it.version.startsWith("$requested.") }
                .maxWithOrNull { a, b -> compareVersions(a.version, b.version) }

        fun standardLocations(windowsProgramDirs: List<String>): List<InstallationLocation> {
            val directories = if (System.getProperty("os.name").startsWith("Windows")) {
                windowsProgramDirs.map { File(it, "1cv8") }
            } else {
                // One directory per architecture, e.g. /opt/1cv8/x86_64.
                File("/opt/1cv8").listFiles { file -> file.isDirectory }.orEmpty().sorted()
            }
            return directories.map { InstallationLocation(it, "standard location") }
        }

        fun parseList(list: String): List<String> =
            list.split(',').map { it.trim() }.filter { it.isNotEmpty() }

        private fun installation(home: File, source: String): PlatformInstallation? {
            val ibcmd = ibcmdIn(home) ?: return null
            // A link such as /opt/1cv8/current is named after the version only at its target.
            // toRealPath and not canonicalFile, which does not follow links on Windows.
            val version = listOfNotNull(home.name, home.toPath().toRealPath().fileName?.toString())
                .firstOrNull { versionPattern.matches(it) }
                ?: return null
            return PlatformInstallation(version, home, ibcmd, source)
        }

        private fun ibcmdIn(home: File): File? = ibcmdPaths.map { File(home, it) }.firstOrNull { it.isFile }

        private fun compareVersions(a: String, b: String): Int {
            val left = a.split('.').map { it.toInt() }
            val right = b.split('.').map { it.toInt() }
            for (i in 0 until maxOf(left.size, right.size)) {
                val difference = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
                if (difference != 0) return difference
            }
            return 0
        }
    }
}
