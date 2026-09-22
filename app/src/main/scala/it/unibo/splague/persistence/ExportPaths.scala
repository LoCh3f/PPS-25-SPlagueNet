package it.unibo.splague.persistence

import it.unibo.splague.persistence.FileFormat.{Json, Txt}

import java.nio.file.Path

object ExportPaths:

  def baseDirectory: Path =
    Path.of(System.getProperty("user.home"), "splagnet")

  private def sanitizeFileName(name: String): String =
    name.trim.replaceAll("""[\\/:*?"<>|]""", "_")

  def pathFor(scenarioName: String, fileFormat: FileFormat): Path = fileFormat match
    case Json => baseDirectory.resolve(sanitizeFileName(scenarioName) + ".json")
    case Txt  => baseDirectory.resolve(sanitizeFileName(scenarioName) + ".txt")
