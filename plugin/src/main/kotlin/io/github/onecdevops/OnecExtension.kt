package io.github.onecdevops

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property

abstract class OnecExtension {
    /** Full platform version or its leading part, e.g. `8.3.25`; the newest matching installation is used. */
    abstract val platformVersion: Property<String>

    /** Full 1C:EDT version or its leading part, e.g. `2026.1`; needed only to build 1C:EDT projects. */
    abstract val edtVersion: Property<String>

    /**
     * What the project builds: an XML dump or a 1C:EDT project of a configuration, an extension, an external
     * data processor or report; the format is detected from the files. `src` by default.
     */
    abstract val srcDir: DirectoryProperty
}
