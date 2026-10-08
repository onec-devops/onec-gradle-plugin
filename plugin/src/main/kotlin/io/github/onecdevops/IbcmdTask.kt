package io.github.onecdevops

import org.gradle.api.GradleException
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.Input
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.ByteBuffer
import java.nio.charset.CharacterCodingException
import java.nio.charset.Charset
import javax.inject.Inject

@DisableCachingByDefault(because = "Abstract super-class, not to be instantiated directly")
abstract class IbcmdTask: PlatformTask() {
    /** Version of the installation in use, so that installing another build triggers a rebuild. */
    @get:Input
    val resolvedPlatformVersion: String
        get() = platformVersion.orNull
            ?.let { PlatformInstallation.select(it, PlatformInstallation.discover(installationLocations())) }
            ?.version ?: ""

    @get:Inject
    protected abstract val execOperations: ExecOperations

    @get:Inject
    protected abstract val fileSystemOperations: FileSystemOperations

    protected fun ibcmd(vararg args: String) {
        val executable = findIbcmd()
        val output = ByteArrayOutputStream()
        val result = execOperations.exec { spec ->
            spec.executable = executable.path
            spec.args = args.toList()
            spec.standardOutput = output
            spec.errorOutput = output
            spec.isIgnoreExitValue = true
        }
        val text = decodeProcessOutput(output.toByteArray()).trim()
        if (result.exitValue != 0) {
            throw GradleException("ibcmd ${args.takeWhile { !it.startsWith("--") }.joinToString(" ")} " +
                "failed with exit code ${result.exitValue}:\n$text")
        }
        logger.info(text)
    }

    private fun findIbcmd(): File {
        val requested = platformVersion.orNull ?: throw GradleException("onec.platformVersion is not set")
        val locations = installationLocations()
        val installation = PlatformInstallation.select(requested, PlatformInstallation.discover(locations))
            ?: throw GradleException(
                "1C:Enterprise platform $requested with ibcmd not found, looked in:\n" +
                    locations.joinToString("\n") { "  ${it.path}  (${it.source})" } +
                    "\nList the installations in the Gradle property $INSTALLATION_PATHS_PROPERTY, or name " +
                    "the environment variables that point to them in $INSTALLATION_VARIABLES_PROPERTY " +
                    "($DEFAULT_INSTALLATION_VARIABLE by default).")
        return installation.ibcmd
    }
}

/** ibcmd is expected to write UTF-8; anything else is read in the [fallback] encoding. */
internal fun decodeProcessOutput(
    bytes: ByteArray,
    fallback: Charset = Charset.forName(System.getProperty("native.encoding") ?: Charset.defaultCharset().name())
): String = try {
    Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(bytes)).toString()
} catch (e: CharacterCodingException) {
    String(bytes, fallback)
}
