package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Builds a .cf, .cfe, .epf or .erf file from an XML dump or a 1C:EDT project. */
@CacheableTask
abstract class OnecBuildTask: IbcmdTask(), UsesEdt {
    @get:Internal
    abstract val srcDir: DirectoryProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: FileTree
        get() = srcDir.asFileTree

    /** Version of 1C:EDT that exports the project, so that installing another one triggers a rebuild; empty for XML. */
    @get:Input
    val resolvedEdtVersion: String
        get() = if (sourcesAreEdtProject()) findEdt()?.version ?: "" else ""

    // Not an input: the result does not depend on the template, and ibcmd creates a different one each time,
    // which would change the build cache key.
    @get:Internal
    abstract val infobaseDir: DirectoryProperty

    @get:Internal
    abstract val outputDirectory: DirectoryProperty

    /** Name of the output file without the extension. */
    @get:Internal
    abstract val baseName: Property<String>

    // The file extension follows the kind of the sources. It is read here and not in a provider
    // set up by the plugin, which would make the sources an input of the configuration cache.
    @get:OutputFile
    val outputFile: File
        get() {
            val extension = externalObjectExtension() ?: if (sourcesAreExtension()) "cfe" else "cf"
            return outputDirectory.file("${baseName.get()}.$extension").get().asFile
        }

    @TaskAction
    fun build() {
        val exportDir = File(temporaryDir, "xml")
        val infobaseCopy = File(temporaryDir, "ib")
        val dataDir = File(temporaryDir, "data")
        val output = outputFile
        fileSystemOperations.delete { it.delete(exportDir, infobaseCopy, dataDir, output) }
        try {
            val external = externalObjectExtension() != null
            val source = if (sourcesAreEdtProject()) {
                // 1C:EDT exports an external object to the directory of its kind, which ibcmd takes;
                // in the root of the export it looks for a configuration.
                val exported = exportEdtProject(exportDir)
                edtExternalObject()?.let { File(exported, it.parentFile.name) } ?: exported
            } else {
                srcDir.get().asFile
            }
            val extensionName = if (external) null else extensionName(File(source, "Configuration.xml"))
            fileSystemOperations.copy {
                it.from(infobaseDir)
                it.into(infobaseCopy)
            }
            val infobase = listOfNotNull("--data=$dataDir", "--db-path=$infobaseCopy", extensionName?.let { "--extension=$it" })
            if (external) {
                // An external data processor or report is not a part of the infobase, so it is built
                // straight to the file; the kind of the file follows the XML.
                ibcmd("infobase", "config", "import", *infobase.toTypedArray(), "--out=${output.path}", source.path)
            } else {
                // Loading into the infobase and saving from it takes two calls instead of one
                // "import --out", but that one hardly uses a second core and is several times
                // slower on a large configuration.
                ibcmd("infobase", "config", "import", *infobase.toTypedArray(), source.path)
                ibcmd("infobase", "config", "save", *infobase.toTypedArray(), output.path)
            }
        } catch (e: GradleException) {
            // ibcmd leaves an empty file behind when it fails.
            output.delete()
            throw e
        } finally {
            fileSystemOperations.delete { it.delete(exportDir, infobaseCopy, dataDir) }
        }
    }

    /** Converts the 1C:EDT project to an XML dump in [target], which ibcmd can load. */
    private fun exportEdtProject(target: File): File {
        val requested = edtVersion.orNull ?: throw GradleException("onec.edtVersion is not set")
        val edt = findEdt() ?: throw installationNotFound("1C:EDT $requested with 1cedtcli",
            edtInstallationLocations(windowsProgramDirs.get()), UsesEdt.INSTALLATION_PATHS_PROPERTY,
            UsesEdt.INSTALLATION_VARIABLES_PROPERTY, UsesEdt.DEFAULT_INSTALLATION_VARIABLE)
        // A workspace of its own, as 1C:EDT locks it, and a new one each time, as it remembers the projects.
        val workspace = File(temporaryDir, "workspace")
        val script = File(temporaryDir, "export.txt")
        fileSystemOperations.delete { it.delete(workspace, script) }
        // "export --project" on a project that is not in the workspace yet imports it itself, but then
        // leaves the BSL modules out of the result and still succeeds; an import beforehand avoids that.
        // The script runs both in one 1C:EDT start, which takes most of a minute. Its shell takes
        // backslashes for escapes, hence the forward slashes.
        val project = srcDir.get().asFile
        script.writeText("""
            import --project "${project.invariantSeparatorsPath}"
            export --project-name "${edtProjectName(File(project, ".project"))}" --configuration-files "${target.invariantSeparatorsPath}"
        """.trimIndent())
        try {
            // 1cedtcli writes in the OEM code page of the console unless told otherwise.
            val output = run(edt.executable, "1cedtcli export", listOf("-data", workspace.path,
                "-vmargs", "-Dfile.encoding=UTF-8", "-file", script.path))
            // On Linux 1cedtcli exits with 0 when a command of the script fails, so the result is told by the files.
            if (target.list().isNullOrEmpty()) {
                throw GradleException("1cedtcli export exported nothing:\n$output")
            }
        } finally {
            fileSystemOperations.delete { it.delete(workspace, script) }
        }
        return target
    }

    private fun edtProjectName(projectFile: File): String {
        if (!projectFile.isFile) {
            throw GradleException("$projectFile not found")
        }
        val description = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(projectFile).documentElement
        return (0 until description.childNodes.length).map { description.childNodes.item(it) }
            .firstOrNull { it.nodeName == "name" }?.textContent
            ?: throw GradleException("$projectFile has no project name")
    }

    private fun findEdt(): Installation? = edtVersion.orNull
        ?.let { EdtInstallations.select(it, EdtInstallations.discover(edtInstallationLocations(windowsProgramDirs.get()))) }

    private fun configurationXml(): File = srcDir.file("Configuration.xml").get().asFile

    /** A 1C:EDT project is a directory with its manifest, the sources being in its `src`. */
    private fun sourcesAreEdtProject(): Boolean = srcDir.file("DT-INF/PROJECT.PMF").get().asFile.isFile

    /** `epf` or `erf` if the sources are an external data processor or report, null otherwise. */
    private fun externalObjectExtension(): String? =
        if (sourcesAreEdtProject()) {
            edtExternalObject()?.let { EDT_EXTERNAL_OBJECT_KINDS.getValue(it.parentFile.name) }
        } else {
            xmlExternalObjectExtension()
        }

    /**
     * The directory of the external data processor or report in a 1C:EDT project of external objects, null for
     * other projects. Such a project can hold several of them, but a build makes one file, so it must hold one.
     */
    private fun edtExternalObject(): File? {
        if (!edtProjectHasNature("V8ExternalObjectsNature")) return null
        val objects = EDT_EXTERNAL_OBJECT_KINDS.keys.flatMap { kind ->
            srcDir.dir("src/$kind").get().asFile.listFiles { file -> file.isDirectory }.orEmpty().sorted()
        }
        return objects.singleOrNull() ?: throw GradleException("The 1C:EDT project ${srcDir.get().asFile} " +
            "must hold one external data processor or report to build, it holds ${objects.size}" +
            objects.joinToString(prefix = if (objects.isEmpty()) "" else ": ") { "${it.parentFile.name}/${it.name}" })
    }

    /** `<Name>.xml` describing an external data processor or report next to the `<Name>` directory, without Configuration.xml. */
    private fun xmlExternalObjectExtension(): String? {
        if (configurationXml().isFile) return null
        val descriptions = srcDir.get().asFile.listFiles { file -> file.isFile && file.extension == "xml" }.orEmpty()
        return descriptions.sorted().firstNotNullOfOrNull { file ->
            val text = file.readText()
            when {
                text.contains("<ExternalDataProcessor ") -> "epf"
                text.contains("<ExternalReport ") -> "erf"
                else -> null
            }
        }
    }

    private fun sourcesAreExtension(): Boolean =
        if (sourcesAreEdtProject()) {
            edtProjectHasNature("V8ExtensionNature")
        } else {
            configurationXml().let { it.isFile && it.readText().contains("<ConfigurationExtensionPurpose>") }
        }

    private fun edtProjectHasNature(nature: String): Boolean =
        srcDir.file(".project").get().asFile.let { it.isFile && it.readText().contains(nature) }

    /** Name of the extension described by [configurationXml], null for a configuration. */
    private fun extensionName(configurationXml: File): String? {
        if (!configurationXml.isFile) {
            throw GradleException("$configurationXml not found")
        }
        val properties = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(configurationXml).getElementsByTagName("Properties").item(0)
            ?: throw GradleException("$configurationXml is not a configuration description")
        val values = (0 until properties.childNodes.length)
            .map { properties.childNodes.item(it) }
            .associate { it.nodeName to it.textContent }
        return if ("ConfigurationExtensionPurpose" in values) values["Name"] else null
    }
}

/** Directories of the external objects in `src` of a 1C:EDT project, and in its export, with the files they build to. */
private val EDT_EXTERNAL_OBJECT_KINDS = mapOf("ExternalDataProcessors" to "epf", "ExternalReports" to "erf")
