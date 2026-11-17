# Onec Gradle Plugin

**English** | [Русский](README.ru.md)

[![Build](https://github.com/onec-devops/onec-gradle-plugin/actions/workflows/build.yml/badge.svg?branch=main)](https://github.com/onec-devops/onec-gradle-plugin/actions/workflows/build.yml)
[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.onec-devops.onec?logo=gradle&logoColor=4DC9C0&label=Plugin%20Portal)](https://plugins.gradle.org/plugin/io.github.onec-devops.onec)
[![Documentation](https://img.shields.io/badge/Documentation-grey?logo=docusaurus)](https://onec-devops.github.io/onec-gradle-plugin/)

A Gradle plugin for projects on the 1C:Enterprise platform. It automates development processes
and lets projects follow the best engineering practices.

## Features

- **Any format and project type**. The plugin builds configurations, extensions, external data
  processors and reports from an XML dump or a 1C:EDT project and detects which one it has.
- **Multi-project builds**. A configuration, its extensions, data processors and reports can be
  kept in one repository and built as one solution.
- **Customization and extensibility**. The plugin settings can be changed and custom tasks added,
  such as a code check with BSL Language Server.
- **Gradle ecosystem integration**. A 1C project is compatible with Gradle community plugins and
  with IDEs and CI systems that support Gradle.

## Getting started

1. Install Java 17 or newer and the latest Gradle by following the
   [Gradle instructions](https://gradle.org/install/).
2. If the project has no Gradle build yet, create one in the project directory. The command
   creates the build scripts and the Gradle Wrapper:

   ```bash
   gradle init --type basic --dsl kotlin --use-defaults
   ```

3. Put the XML dump or the 1C:EDT project in the `src` directory:

   ```text
   my-project/
   ├─ gradlew
   ├─ settings.gradle.kts
   ├─ build.gradle.kts
   └─ src/
      ├─ Configuration.xml   # the XML dump
      └─ ...
   ```

   For a 1C:EDT project, `src` holds the project's `.project`, `DT-INF/` and `src/`.

4. Apply the plugin in `build.gradle.kts`:

   ```kotlin
   plugins {
       id("io.github.onec-devops.onec") version "0.2.0"
   }

   onec {
       platformVersion = "8.3.25"  // the version of the installed platform or its leading part
       edtVersion = "2026.2"       // only for a 1C:EDT project
       srcDir = file("src")        // the sources directory, src by default
   }
   ```

5. Build the project with `./gradlew build`. The result appears in `build/onec/`.

Learn more about the plugin in the
[documentation](https://onec-devops.github.io/onec-gradle-plugin/).

## Contributing

Pull requests are welcome: bug fixes, new features, documentation improvements. A larger change
is better discussed in an issue first. [CONTRIBUTING.md](CONTRIBUTING.md) explains how to build
the plugin and run its tests.

## License

The plugin is distributed under the [Apache License 2.0](LICENSE).
