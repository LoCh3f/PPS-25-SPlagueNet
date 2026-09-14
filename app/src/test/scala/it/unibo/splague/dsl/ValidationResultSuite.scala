package it.unibo.splague.dsl

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
final class ValidationResultSuite extends AnyFunSuite with Matchers:

  test("partition should collect all errors and all successes separately"):
    val results: List[ValidationResult[Int]] =
      List(Right(1), Left(List("bad-a")), Right(2), Left(List("bad-b", "bad-c")))

    val (errors, values) = ValidationResult.partition(results)

    errors shouldBe List("bad-a", "bad-b", "bad-c")
    values shouldBe List(1, 2)

  test("partition on an all-success list should return no errors"):
    val (errors, values) = ValidationResult.partition(List(Right(1), Right(2)))

    errors shouldBe empty
    values shouldBe List(1, 2)
