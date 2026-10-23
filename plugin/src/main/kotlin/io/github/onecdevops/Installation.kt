package io.github.onecdevops

import java.io.File
import java.util.Properties

/** A place to look for installations, with the setting it comes from. */
internal class InstallationLocation(val path: File, val source: String)

/** An installed program: the platform with ibcmd or 1C:EDT with 1cedtcli. */
internal class Installation(val version: String, val home: File, val executable: File, val source: String)

/** How to recognize an installation of a program and its version. */
internal abstract class InstallationKind {
    /** The executable of the installation in [home], null if [home] is not one. */
    protected abstract fun executableIn(home: File): File?

    protected abstract fun versionOf(home: File): String?

    /** Directories under [directory] that may be the home of an installation, [directory] itself included. */
    protected open fun homesIn(directory: File): List<File> = listOf(directory)

    /**
     * Lists the installations found in [locations], in their order. A location is either
     * one installation or a directory with an installation per version.
     */
    fun discover(locations: List<InstallationLocation>): List<Installation> =
        locations.flatMap { location ->
            val single = installation(location.path, location.source)
            if (single != null || homesIn(location.path).any { executableIn(it) != null }) {
                listOfNotNull(single)
            } else {
                location.path.listFiles { file -> file.isDirectory }.orEmpty().sorted()
                    .mapNotNull { installation(it, location.source) }
            }
        }.distinctBy { it.executable.toPath().toRealPath() }

    /**
     * Picks the newest of [installations] matching [requested], which is either a full version
     * (`8.3.25.1633`) or its leading part (`8.3.25`); of equal versions the earlier one wins.
     */
    fun select(requested: String, installations: List<Installation>): Installation? =
        installations
            .filter { it.version == requested || it.version.startsWith("$requested.") }
            .maxWithOrNull { a, b -> compareVersions(a.version, b.version) }

    private fun installation(directory: File, source: String): Installation? =
        homesIn(directory).firstNotNullOfOrNull { home ->
            val executable = executableIn(home) ?: return@firstNotNullOfOrNull null
            versionOf(home)?.takeIf { versionPattern.matches(it) }
                ?.let { Installation(it, home, executable, source) }
        }

    protected fun firstFile(home: File, paths: List<String>): File? = paths.map { File(home, it) }.firstOrNull { it.isFile }

    private fun compareVersions(a: String, b: String): Int {
        val left = a.split('.').map { it.toInt() }
        val right = b.split('.').map { it.toInt() }
        for (i in 0 until maxOf(left.size, right.size)) {
            val difference = left.getOrElse(i) { 0 }.compareTo(right.getOrElse(i) { 0 })
            if (difference != 0) return difference
        }
        return 0
    }

    companion object {
        val versionPattern = Regex("""\d+(\.\d+)*""")
    }
}

/** The 1C:Enterprise platform: a directory per version with ibcmd, named after the version. */
internal object PlatformInstallations: InstallationKind() {
    override fun executableIn(home: File): File? = firstFile(home, listOf("bin/ibcmd.exe", "ibcmd"))

    // A link such as /opt/1cv8/current is named after the version only at its target.
    // toRealPath and not canonicalFile, which does not follow links on Windows.
    override fun versionOf(home: File): String? =
        listOfNotNull(home.name, home.toPath().toRealPath().fileName?.toString())
            .firstOrNull { versionPattern.matches(it) }

    fun standardLocations(windowsProgramDirs: List<String>): List<InstallationLocation> {
        val directories = if (isWindows()) {
            windowsProgramDirs.map { File(it, "1cv8") }
        } else {
            // One directory per architecture, e.g. /opt/1cv8/x86_64.
            File("/opt/1cv8").listFiles { file -> file.isDirectory }.orEmpty().sorted()
        }
        return directories.map { InstallationLocation(it, "standard location") }
    }
}

/**
 * 1C:EDT with 1cedtcli. The 1C:Enterprise installer puts each version in a directory of its own,
 * 1C:EDT Start puts it in the `1cedt` subdirectory of one; the version is in `configuration/config.ini`.
 */
internal object EdtInstallations: InstallationKind() {
    override fun executableIn(home: File): File? = firstFile(home, listOf("1cedtcli.exe", "1cedtcli"))

    override fun versionOf(home: File): String? {
        val config = File(home, "configuration/config.ini").takeIf { it.isFile } ?: return null
        return Properties().apply { config.reader().use { load(it) } }.getProperty("product.version")
    }

    override fun homesIn(directory: File): List<File> = listOf(directory, File(directory, "1cedt"))

    fun standardLocations(localAppData: String, windowsProgramDirs: List<String>): List<InstallationLocation> {
        val directories = if (isWindows()) {
            listOfNotNull(localAppData.takeIf { it.isNotEmpty() }?.let { File(it, "1C/1cedtstart/installations") }) +
                windowsProgramDirs.map { File(it, "1C/1CE/components") }
        } else {
            listOf(File("/opt/1C/1CE/components"))
        }
        return directories.map { InstallationLocation(it, "standard location") }
    }
}

/**
 * Where to look for installations, in this order: [paths] from the Gradle property [pathsProperty],
 * the environment [variables] that are set, the [standard] locations.
 */
internal fun locationsFrom(
    paths: List<String>,
    pathsProperty: String,
    variables: List<String>,
    standard: List<InstallationLocation>
): List<InstallationLocation> =
    paths.map { InstallationLocation(File(it), pathsProperty) } +
        variables.mapNotNull { name ->
            System.getenv(name)?.takeIf { it.isNotBlank() }
                ?.let { InstallationLocation(File(it), "environment variable $name") }
        } +
        standard

internal fun parseList(list: String): List<String> =
    list.split(',').map { it.trim() }.filter { it.isNotEmpty() }

private fun isWindows() = System.getProperty("os.name").startsWith("Windows")
