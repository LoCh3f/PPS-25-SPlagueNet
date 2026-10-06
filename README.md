# PPS-25-SPlagueNet

![SPlagueNet preview](assets/preview.jpeg)

This project is a Scala application built with Gradle. The following steps explain how to build it, create a runnable distribution, and run it locally.

## Overview

SPlagueNet is a Scala 3 desktop application that simulates malware ("plague") outbreaks spreading across a network topology. You build a scenario — a network of nodes and connections, a malware definition, and an awareness/countermeasure configuration — either through the GUI editor or a small internal DSL, then run a tick-based simulation and watch the infection spread, get detected, and get contained (or not) across the network.

Key capabilities:

- **Interactive topology editor** — build and edit the network graph directly on a canvas (add/move/remove nodes and connections; see [User Guide](#user-guide) below for the full command reference).
- **Tick-based simulation** — step through infection, detection, defense, prevention, cure, and destruction events over a configurable number of iterations.
- **Scenario reports** — inspect per-tick summaries and milestones once a simulation finishes.
- **Import/export** — save and load scenarios and reports as JSON or plain-text files.

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

## User Guide

Reference for interacting with the network topology canvas in the GUI.

### Comandi

| Azione                      | Comando                                                                |
|------------------------------|-------------------------------------------------------------------------|
| Zoom in / out                | Rotellina mouse avanti / indietro                                       |
| Pan                          | Click su spazio vuoto + trascina                                        |
| Seleziona nodo                | Click sinistro su nodo                                                  |
| Seleziona connessione         | Click sinistro su linea                                                 |
| Sposta nodo                   | Click su nodo + trascina                                                |
| Aggiungi nodo                 | **Ctrl** + click su spazio vuoto → compila il form → **Save**           |
| Modifica nodo                 | Click destro su nodo → modifica il form → **Save**                      |
| Rimuovi nodo                  | Seleziona nodo + **Delete**                                             |
| Crea connessione              | **Shift** + click su nodo sorgente, trascina, rilascia su destinazione → compila il form → **Save** |
| Modifica connessione          | Click destro su connessione → modifica il form → **Save**               |
| Rimuovi connessione           | Seleziona connessione + **Delete**                                      |
