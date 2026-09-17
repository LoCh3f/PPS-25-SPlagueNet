package it.unibo.splague.persistence

sealed trait FileFormat
object FileFormat:
  sealed trait Json extends FileFormat
  sealed trait Txt extends FileFormat
  sealed trait Html extends FileFormat
