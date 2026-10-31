# Contributing

Pull requests are welcome: bug fixes, new features, documentation. For a larger change, open an
issue first to agree on the approach.

## Sending a change

- Base the branch on `main` and keep each commit to one change.
- Write the commit subject in the imperative and about what the change gives the user, for
  example `Build from XML dumps`.
- Add tests: unit tests for logic that needs no platform, functional tests for the behaviour of
  a build.
- Keep code, test names, fixtures and commit messages in English.
- Make sure `./gradlew build` passes.

## How a build works

`ibcmd` works only with an existing infobase, so `onecCreateInfobase` creates an empty one in
`build/onec/infobase` once. `onecBuild` copies it into its temporary directory and gives `ibcmd`
a data directory there, so the infobase stays empty.

What the sources are is read from the files when the task runs, not when the build is
configured, so that changing the sources does not invalidate the configuration cache.

- A 1C:EDT project is first exported to an XML dump with `1cedtcli`, in a workspace of its own
  in the temporary directory, and then built as the dump. The export script imports the project
  into the workspace first: an export of a project that is not there leaves out the BSL modules.
- A configuration or an extension is imported into the infobase copy and saved from it to the
  file. This takes two `ibcmd` calls, but on a large configuration it is several times faster
  than building the file directly.
- An external data processor or report is not a part of an infobase and is built directly to the
  file with `ibcmd infobase config import --out`.

The inputs of a build task are the source files and the full versions of the platform and, for a
1C:EDT project, of 1C:EDT in use, so installing another build of either rebuilds the files. The
infobase is not an input: the result does not depend on it, and `ibcmd` creates a different one
each time, which would change the build cache key.

## Building and testing

You need Java 17 or newer, the 1C:Enterprise platform with `ibcmd` and 1C:EDT 2026 with
`1cedtcli`: the functional tests call them and fail without them. If the platform is not in a
standard location, point `ONEC_HOME` or the Gradle property `onec.platform.installations.paths` to it;
for 1C:EDT, `EDT_HOME` or `onec.edt.installations.paths`.

`./gradlew build` compiles and validates the plugin and runs the unit and the functional tests.
`./gradlew test` runs only the unit tests, `./gradlew functionalTest` only the functional ones.

Each functional test creates a project in a temporary directory, copies a fixture into it and
runs a build with Gradle TestKit; four tests run at a time.

A fixture is an XML dump in the form the platform exports it; after editing one by hand, load it
with `ibcmd` and export it again. The fixtures are exported by the oldest platform version under
test, with its compatibility mode, because a platform does not load a dump made by a newer one.

The tests of 1C:EDT projects make the projects from the same XML fixtures with `1cedtcli`, once a
run, for the version of the platform in use. 1C:EDT does not start a project for a platform
version it does not support but hangs, and the 1C:EDT of an onec-docker image supports only the
platform of the image, so projects kept in the repository would hang on all but one image.

The functional tests run their builds on the same Gradle version as the build itself. To run them
on another one, pass it as a property:

```
./gradlew functionalTest -PtestGradleVersion=9.0.0
```

The tests ask for any platform 8.3 and any 1C:EDT 2026. To test other versions, pass them the
same way, in full or by their leading part:

```
./gradlew functionalTest -PtestPlatformVersion=8.3.25.1633 -PtestEdtVersion=2026.1.3
```

## CI

The [Build](.github/workflows/build.yml) workflow runs on pushes to `main` and on pull requests.
It runs the unit tests and validates the plugin on Linux and Windows, and runs the functional
tests on Linux in the images of the platform and 1C:EDT of
[onec-docker](https://github.com/onec-devops/onec-docker), one job for each pair of
`matrix.onec-version` and `matrix.edt-version`, which the job passes to the tests as
`testPlatformVersion` and `testEdtVersion`; the job of the oldest platform runs the test builds
on the oldest supported Gradle. The [Release](.github/workflows/release.yml)
workflow runs the Build workflow and publishes the plugin only if it passes. It publishes the
`version` of `gradle.properties`; the examples in `README.md` and `README_ru.md` name the version
too, so the first change after a release raises it in all three.

The images are the private packages `1cv8-server` of the repository owner in the GitHub Container
registry, for example `ghcr.io/onec-devops/1cv8-server:8.3.25.1633-edt-2026.1.3`. The repository gets read
access to them under "Manage Actions access" in the package settings. In a fork the workflow
takes the images of the fork owner, built by a fork of onec-docker.

## License

By contributing, you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE). Source files carry no license header; the `LICENSE` file covers
the whole repository.
