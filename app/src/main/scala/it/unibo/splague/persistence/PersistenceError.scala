package it.unibo.splague.persistence

enum PersistenceError:
  case IO(message: String)
  case Parsing(message: String)
