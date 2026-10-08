package io.github.onecdevops

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File

/**
 * Creates the infobase that ibcmd requires to exist. Builds never change it
 * and work on their own copies, so they cannot lock each other.
 */
@DisableCachingByDefault(because = "Not worth caching")
abstract class OnecCreateInfobaseTask: IbcmdTask() {
    @get:OutputDirectory
    abstract val infobaseDir: DirectoryProperty

    @TaskAction
    fun create() {
        val dataDir = File(temporaryDir, "data")
        fileSystemOperations.delete { it.delete(infobaseDir, dataDir) }
        try {
            ibcmd("infobase", "create", "--data=$dataDir", "--db-path=${infobaseDir.get().asFile}")
        } finally {
            // ibcmd preallocates about 500 MB of session logs there.
            fileSystemOperations.delete { it.delete(dataDir) }
        }
    }
}
