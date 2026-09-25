package it.unibo.splague.utils

import it.unibo.splague.dsl.*
import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.ChannelType.LAN
import it.unibo.splague.model.malware.MalwareKind.Worm
import it.unibo.splague.model.malware.PayloadSeverityLevel.Low
import it.unibo.splague.model.malware.PropagationVector.NetworkExploit
import it.unibo.splague.model.node.NodeType.Workstation

// $COVERAGE-OFF$
/** A minimal, easy-to-follow scenario: four workstations chained in a straight line (`n1 -> n2 ->
  * n3 -> n4`), with `n1` as the outbreak's starting node. Meant as a small, predictable alternative
  * to [[ExampleScenario]] for trying out the simulation.
  *
  * Nodes have no defense/patch and the malware has maximum infectivity, so the outbreak reliably
  * marches down the chain one hop at a time instead of depending on a coin flip per node. Nodes
  * also carry the same `NetworkExploit` vector as the malware (the DSL's default), so infection can
  * actually propagate down the chain: without a shared vector it would never spread past the seeded
  * starting node.
  *
  * The seed is fixed on purpose: this scenario is meant to be predictable, unlike a DSL scenario
  * that leaves it undeclared.
  */
object SimpleScenario:
  def linearScenario(): Either[String, Scenario] =
    val result: ValidationResult[Scenario] =
      scenario("Linear chain"):
        seed(1)
        maxIterations(20)

        network:
          node("n1", Workstation)
          node("n2", Workstation)
          node("n3", Workstation)
          node("n4", Workstation)
          "n1" <-> "n2" via LAN
          "n2" <-> "n3" via LAN
          "n3" <-> "n4" via LAN

        malware(
          "SimpleWorm",
          Worm,
          infectivity = 1.0,
          stealth = 0.3,
          severity = Low,
          persistence = 0.5,
          footprint = 0.3,
          vectors = Set(NetworkExploit)
        )

        startingNode("n1")

    result.left.map(_.mkString("; "))
// $COVERAGE-ON$
