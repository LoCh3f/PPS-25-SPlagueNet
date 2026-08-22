package it.unibo.splague.model

import org.junit.runner.RunWith
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers.shouldBe
import org.scalatestplus.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class NodeTest extends AnyFunSuite:
  test("NodeType values and coefficients should match expected values"):
    NodeType.IoTDevice.detectionCoefficient shouldBe 0.3
    NodeType.IoTDevice.structuralVulnerability shouldBe 1.3

    NodeType.Workstation.detectionCoefficient shouldBe 1.5
    NodeType.Workstation.structuralVulnerability shouldBe 0.8

    NodeType.Router.detectionCoefficient shouldBe 0.3
    NodeType.Router.structuralVulnerability shouldBe 1.3

    NodeType.Server.detectionCoefficient shouldBe 0.3
    NodeType.Server.structuralVulnerability shouldBe 1.3

    NodeType.MobileDevice.detectionCoefficient shouldBe 0.3
    NodeType.MobileDevice.structuralVulnerability shouldBe 1.3
