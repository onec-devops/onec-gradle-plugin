# Onec Gradle Plugin

English | [Русский](README_ru.md)

A Gradle plugin for 1C:Enterprise development. It builds configurations, extensions, external
data processors and reports.

## Requirements

- Gradle 9.0 or newer.
- Java 17 or newer.
- The 1C:Enterprise platform with `ibcmd`.

## Getting started

The steps below create a project that builds a configuration.

1. [Install Gradle](https://gradle.org/install/) 9.0 or newer.

2. Create an empty directory for the project, for example `my-project`, and run in it:

   ```
   gradle init --type basic --dsl kotlin --use-defaults
   ```

   The command creates a Gradle build
   ([`gradle init` documentation](https://docs.gradle.org/current/userguide/build_init_plugin.html)).
3. Dump the configuration to XML files in `src`.
   `Configuration.xml` must end up directly in `src`:

   ```
   my-project/
   ├─ settings.gradle.kts
   ├─ build.gradle.kts
   └─ src/
      ├─ Configuration.xml
      └─ ...
   ```

4. Replace the contents of `build.gradle.kts` with:

   ```kotlin
   plugins {
       id("io.github.onec-devops.onec") version "0.1.0"
   }

   onec {
       platformVersion = "8.3.25"   // the version of the installed platform or its leading part
   }
   ```

5. Build the project:

   ```
   ./gradlew build
   ```

   On Windows, run `gradlew build` in the command prompt or `.\gradlew build` in PowerShell.
   The result is `build/onec/my-project.cf`.

## Configuration

The plugin is configured in the `onec` block of `build.gradle.kts`:

```kotlin
onec {
    platformVersion = "8.3.25"   // required: a full version or its leading part
    srcDir = file("src")         // optional: src by default
}
```

### Build output

A project builds one artifact from the XML dump in `srcDir`. The plugin tells what it is by the
files there:

| `srcDir` contains | Result |
|---|---|
| `Configuration.xml` of a configuration | `.cf` |
| `Configuration.xml` of an extension | `.cfe` |
| `<Name>.xml` of an external data processor and the `<Name>` directory | `.epf` |
| `<Name>.xml` of an external report and the `<Name>` directory | `.erf` |

> [!TIP]
> For consistency, an external data processor is best named `DataProcessor` and an external
> report `Report`. Then `srcDir` always holds a file with the same name: `DataProcessor.xml`,
> `Report.xml` or `Configuration.xml`.

The artifact is named after the project: `build/onec/<project name>.cf`. A different name can be set
in `build.gradle.kts`:

```kotlin
base {
    archivesName = "accounting"
}
```

### Finding the platform

`platformVersion` is a full platform version or its leading part. `8.3.25` matches any 8.3.25.x
installation, and the plugin takes the newest of them.

The plugin finds the platform in the standard locations by itself: `%ProgramFiles%\1cv8` and
`%ProgramFiles(x86)%\1cv8` on Windows, `/opt/1cv8/*` on Linux. If it is installed elsewhere, the
path to it is set in Gradle properties, for example in `~/.gradle/gradle.properties`, or through an
environment variable:

| Property | Default | Description | Example |
|---|---|---|---|
| `onec.installations.paths` | | Paths to installations, or to directories with installations of several versions, separated by commas. | `D:/1cv8,E:/1C/8.3.25.1633` |
| `onec.installations.fromEnv` | `ONEC_HOME` | Names of environment variables holding such paths, separated by commas. | `ONEC_HOME,V8_HOME` |

`./gradlew onecInstallations` shows what was found and marks the installation in use:

```
Requested: 8.3.25

* 8.3.25.1633   C:\Program Files\1cv8\8.3.25.1633   standard location
```

## Multi-project builds

A project builds one artifact, so a configuration, an extension to it and an external data
processor are three subprojects of one build. The `my-project` directory is created by the same
`gradle init --type basic --dsl kotlin --use-defaults`. It brings the subprojects together and
builds nothing itself, and the subprojects are plain directories in it:

```
my-project/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ configuration/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ Configuration.xml
│     └─ ...
├─ extension/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ Configuration.xml
│     └─ ...
└─ data-processor/
   ├─ build.gradle.kts
   └─ src/
      ├─ DataProcessor.xml
      └─ DataProcessor/
```

`settings.gradle.kts` includes the subprojects:

```kotlin
rootProject.name = "my-project"

include("configuration", "extension", "data-processor")
```

`build.gradle.kts` in `my-project` declares the plugin with its version but does not apply it
(`apply false`):

```kotlin
plugins {
    id("io.github.onec-devops.onec") version "0.1.0" apply false
}
```

Each subproject applies the plugin in its own `build.gradle.kts`, without the version:

```kotlin
plugins {
    id("io.github.onec-devops.onec")
}

onec {
    platformVersion = "8.3.25"
}
```

> [!NOTE]
> With many subprojects, shared settings such as `platformVersion` are usually moved to a
> convention plugin, see
> [Gradle best practices](https://docs.gradle.org/current/userguide/best_practices_structuring_builds.html).

`./gradlew build` builds all three:

```
configuration/build/onec/configuration.cf
extension/build/onec/extension.cfe
data-processor/build/onec/data-processor.epf
```

## Tasks

| Task | Description |
|---|---|
| `onecBuild` | Builds the project's artifact into `build/onec`. |
| `onecCreateInfobase` | Creates the empty infobase that `ibcmd` needs in `build/onec/infobase`. |
| `onecInstallations` | Displays the detected platform installations. |

`assemble`, and so `build`, depend on `onecBuild`. It is skipped when the sources have not
changed, and with the build cache enabled its result is taken from the cache. The plugin works
with the configuration cache.

## Contributing

Pull requests are welcome: bug fixes, new features, documentation improvements. A larger change
is better discussed in an issue first. [CONTRIBUTING.md](CONTRIBUTING.md) explains how to build
the plugin and run its tests.

## License

The plugin is distributed under the [Apache License 2.0](LICENSE).
