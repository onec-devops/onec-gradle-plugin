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
`build/onec/infobase` once. Each `onecBuild*` task copies it into its own temporary directory and
gives `ibcmd` its own data directory, so that builds of several source sets do not lock each
other.

What a source set holds is read from its files when the task runs, not when the build is
configured, so that changing the sources does not invalidate the configuration cache.

- A configuration or an extension is imported into the infobase copy and saved from it to the
  file. This takes two `ibcmd` calls, but on a large configuration it is several times faster
  than building the file directly.
- An external data processor or report is not a part of an infobase and is built directly to the
  file with `ibcmd infobase config import --out`.

The inputs of a build task are the source files and the full version of the platform in use, so
installing another platform build rebuilds the files. The infobase is not an input: the result
does not depend on it, and `ibcmd` creates a different one each time, which would change the
build cache key.

## Building and testing

You need Java 17 or newer and the 1C:Enterprise platform with `ibcmd`: the functional tests call
the real `ibcmd` and fail without it. If the platform is not in a standard location, point
`ONEC_HOME` or the Gradle property `onec.installations.paths` to it.

`./gradlew build` compiles and validates the plugin and runs the unit and the functional tests.
`./gradlew test` runs only the unit tests, `./gradlew functionalTest` only the functional ones.

Each functional test creates a project in a temporary directory, copies a fixture into it and
runs a build with Gradle TestKit; four tests run at a time.

A fixture is an XML dump in the form the platform exports it; after editing one by hand, load it
with `ibcmd` and export it again. The fixtures are exported by the oldest platform version under
test, with its compatibility mode, because a platform does not load a dump made by a newer one.

The functional tests run their builds on the same Gradle version as the build itself. To run them
on another one, pass it as a property:

```
./gradlew functionalTest -PtestGradleVersion=9.0.0
```

## CI

The [Build](.github/workflows/build.yml) workflow runs on pushes to `main` and on pull requests.
It runs the unit tests and validates the plugin on Linux and Windows, and runs the functional
tests on Linux in the platform images of [onec-docker](https://github.com/onec-devops/onec-docker),
one job for each version in `matrix.onec-version`. The [Release](.github/workflows/release.yml)
workflow runs the Build workflow and publishes the plugin only if it passes.

The images are the private packages `1cv8-server` of the repository owner in the GitHub Container
registry, for example `ghcr.io/onec-devops/1cv8-server:8.3.25.1633`. The repository gets read
access to them under "Manage Actions access" in the package settings. In a fork the workflow
takes the images of the fork owner, built by a fork of onec-docker.

## License

By contributing, you agree that your contribution is licensed under the
[Apache License 2.0](LICENSE). Source files carry no license header; the `LICENSE` file covers
the whole repository.
