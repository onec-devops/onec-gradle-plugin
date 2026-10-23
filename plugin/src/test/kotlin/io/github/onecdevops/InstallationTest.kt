package io.github.onecdevops

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class InstallationTest {

    @field:TempDir
    lateinit var root: File

    @Test fun `finds the build requested in full among several`() {
        install(root, "8.3.25.1633")
        install(root, "8.3.25.1700")

        val found = find("8.3.25.1633", root)

        assertEquals("8.3.25.1633", found?.version)
    }

    @ParameterizedTest
    @ValueSource(strings = ["8.3.25", "8.3"])
    fun `finds the newest build for a leading part of the version`(requested: String) {
        install(root, "8.3.9.2309")
        install(root, "8.3.25.200")
        install(root, "8.3.25.1633")
        install(root, "8.5.1.100")

        val found = find(requested, root)

        assertEquals("8.3.25.1633", found?.version)
    }

    @Test fun `does not match a version that merely starts with the same digits`() {
        install(root, "8.3.250.1")

        val found = find("8.3.25", root)

        assertNull(found)
    }

    @Test fun `ignores directories without ibcmd`() {
        root.resolve("8.3.25.1633/bin").mkdirs()

        val found = find("8.3.25", root)

        assertNull(found)
    }

    @ParameterizedTest
    @ValueSource(strings = ["bin/ibcmd.exe", "ibcmd"])
    fun `finds ibcmd in the Windows and the Linux layout`(layout: String) {
        val ibcmd = install(root, "8.3.25.1633", layout)

        val found = find("8.3.25", root)

        assertEquals(ibcmd, found?.executable)
    }

    @Test fun `accepts a location that is an installation itself`() {
        install(root, "8.3.25.1633")
        install(root, "8.3.25.1700")

        val found = PlatformInstallations.discover(listOf(location(root.resolve("8.3.25.1633"))))

        assertEquals(listOf("8.3.25.1633"), found.map { it.version })
    }

    @Test fun `takes the version of a link from its target`() {
        install(root, "8.3.25.1633", "ibcmd")
        val link = root.resolve("current").toPath()
        try {
            Files.createSymbolicLink(link, root.resolve("8.3.25.1633").toPath())
        } catch (e: IOException) {
            assumeTrue(false, "symbolic links cannot be created here: $e")
        }

        val found = PlatformInstallations.discover(listOf(location(link.toFile())))

        assertEquals(listOf("8.3.25.1633"), found.map { it.version })
    }

    @Test fun `prefers the earlier location for equal versions`() {
        val first = install(root.resolve("first"), "8.3.25.1633")
        install(root.resolve("second"), "8.3.25.1633")

        val found = find("8.3.25", root.resolve("first"), root.resolve("second"))

        assertEquals(first, found?.executable)
    }

    @Test fun `lists an installation found in two locations once`() {
        install(root, "8.3.25.1633")

        val found = PlatformInstallations.discover(listOf(location(root.resolve("8.3.25.1633")), location(root)))

        assertEquals(1, found.size)
    }

    @Test fun `parses a comma separated list ignoring spaces and empty items`() {
        val parsed = parseList(" D:/1cv8, /opt/1cv8/current ,")

        assertEquals(listOf("D:/1cv8", "/opt/1cv8/current"), parsed)
    }

    @Test fun `parses an empty string as an empty list`() {
        val parsed = parseList("")

        assertEquals(emptyList(), parsed)
    }

    @Test fun `finds 1C EDT installed by 1C EDT Start`() {
        val cli = installEdt(root.resolve("1C_EDT 2026.1/1cedt"), "2026.1.3")

        val found = findEdt("2026.1", root)

        assertEquals(cli, found?.executable)
        assertEquals("2026.1.3", found?.version)
    }

    @ParameterizedTest
    @ValueSource(strings = ["1cedtcli.exe", "1cedtcli"])
    fun `finds 1C EDT installed by the 1C Enterprise installer`(executable: String) {
        val cli = installEdt(root.resolve("1c-edt-2026.1.3+25-x86_64"), "2026.1.3", executable)

        val found = findEdt("2026.1", root)

        assertEquals(cli, found?.executable)
    }

    @Test fun `accepts a location that is a 1C EDT Start installation itself`() {
        val cli = installEdt(root.resolve("1C_EDT 2026.1/1cedt"), "2026.1.3")

        val found = findEdt("2026.1", root.resolve("1C_EDT 2026.1"))

        assertEquals(cli, found?.executable)
    }

    @Test fun `finds the newest 1C EDT for a leading part of the version`() {
        installEdt(root.resolve("a"), "2025.2.6")
        installEdt(root.resolve("b"), "2026.1.3")
        installEdt(root.resolve("c"), "2026.1.1")
        installEdt(root.resolve("d"), "2026.2.2")

        val found = findEdt("2026.1", root)

        assertEquals("2026.1.3", found?.version)
    }

    @Test fun `ignores 1C EDT without a version in its configuration`() {
        val home = root.resolve("1c-edt")
        home.mkdirs()
        home.resolve("1cedtcli.exe").writeText("")

        val found = EdtInstallations.discover(listOf(location(root)))

        assertEquals(emptyList(), found)
    }

    private fun findEdt(requested: String, location: File): Installation? =
        EdtInstallations.select(requested, EdtInstallations.discover(listOf(location(location))))

    private fun installEdt(home: File, version: String, executable: String = "1cedtcli.exe"): File {
        home.resolve("configuration").mkdirs()
        home.resolve("configuration/config.ini").writeText("product.version=$version\neclipse.buildId=$version.25\n")
        return home.resolve(executable).apply { writeText("") }
    }

    private fun find(requested: String, vararg locations: File): Installation? =
        PlatformInstallations.select(requested, PlatformInstallations.discover(locations.map { location(it) }))

    private fun location(path: File) = InstallationLocation(path, "test")

    private fun install(root: File, version: String, ibcmd: String = "bin/ibcmd.exe"): File {
        val file = root.resolve("$version/$ibcmd")
        file.parentFile.mkdirs()
        file.writeText("")
        return file
    }
}
