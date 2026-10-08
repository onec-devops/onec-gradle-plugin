package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/** Builds a .cf, .cfe, .epf or .erf file from an XML dump. */
@CacheableTask
abstract class OnecBuildTask: IbcmdTask() {
    @get:Internal
    abstract val srcDir: DirectoryProperty

    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val sources: FileTree
        get() = srcDir.asFileTree

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
        val source = srcDir.get().asFile
        val external = externalObjectExtension() != null
        val extensionName = if (external) null else extensionName(configurationXml())

        val infobaseCopy = File(temporaryDir, "ib")
        val dataDir = File(temporaryDir, "data")
        val output = outputFile
        fileSystemOperations.delete { it.delete(infobaseCopy, dataDir, output) }
        fileSystemOperations.copy {
            it.from(infobaseDir)
            it.into(infobaseCopy)
        }
        val infobase = listOfNotNull("--data=$dataDir", "--db-path=$infobaseCopy", extensionName?.let { "--extension=$it" })
        try {
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
            fileSystemOperations.delete { it.delete(infobaseCopy, dataDir) }
        }
    }

    private fun configurationXml(): File = srcDir.file("Configuration.xml").get().asFile

    /**
     * `epf` or `erf` if the sources are an external data processor or report: `<Name>.xml` describing it
     * next to the `<Name>` directory, without Configuration.xml. Null otherwise.
     */
    private fun externalObjectExtension(): String? {
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
        configurationXml().let { it.isFile && it.readText().contains("<ConfigurationExtensionPurpose>") }

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
