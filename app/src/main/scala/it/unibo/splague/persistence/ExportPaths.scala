package it.unibo.splague.persistence

import java.nio.file.Path

object ExportPaths {

  def baseDirectory: Path =
    Path.of(System.getProperty("user.home"), "splagnet")

  def sanitizeFileName(name: String): String =
    name.trim.replaceAll("""[\\/:*?"<>|]""", "_")

  def pathFor(scenarioName: String): Path =
    baseDirectory.resolve(sanitizeFileName(scenarioName) + ".json")

}
