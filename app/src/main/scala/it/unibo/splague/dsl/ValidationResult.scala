package it.unibo.splague.dsl

type ValidationResult[A] = Either[List[String], A]
