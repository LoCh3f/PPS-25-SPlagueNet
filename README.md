# PPS-25-SPlagueNet

This project is a Scala application built with Gradle. The following steps explain how to build it, create a runnable distribution, and run it locally.

## Requirements

- JDK 21 or newer
- Git
- A Unix-like shell or PowerShell

The repository already includes the Gradle wrapper, so you do not need to install Gradle globally.

## 1. Build the project

From the repository root, run:

```bash
./gradlew app:build
```

This compiles the sources, runs the test suite, and generates the build output under `app/build/`.

## 2. Create a runnable distribution

To create an installable application bundle:

```bash
./gradlew app:installDist
```

This generates the runnable app in:

```text
app/build/install/app/bin/
```

You can then start it directly with:

```bash
./app/build/install/app/bin/app
```

On Windows, use:

```powershell
app\build\install\app\bin\app.bat
```

## 3. Run the app from Gradle

During development, the quickest way to run it is:

```bash
./gradlew app:run
```

This launches the application without creating a packaged distribution.

## Useful commands

```bash
./gradlew app:clean
./gradlew app:test
./gradlew app:assemble
```

The default main entry point is defined in `app/src/main/scala/it/unibo/splague/App.scala` and is launched by the `app:run` task.
