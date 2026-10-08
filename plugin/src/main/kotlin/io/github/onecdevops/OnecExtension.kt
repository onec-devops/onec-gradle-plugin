package io.github.onecdevops

import org.gradle.api.provider.Property

abstract class OnecExtension {
    /** Full platform version or its leading part, e.g. `8.3.25`; the newest matching installation is used. */
    abstract val platformVersion: Property<String>
}
