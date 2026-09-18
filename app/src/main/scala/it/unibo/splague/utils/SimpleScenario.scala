package it.unibo.splague.utils

import it.unibo.splague.dsl.*
import it.unibo.splague.model.Probability
import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeType.Workstation
import it.unibo.splague.model.node.Topology

/** A minimal, easy-to-follow scenario: four workstations chained in a straight line (`n1 -> n2 ->
  * n3 -> n4`), with `n1` as the outbreak's starting node. Meant as a small, predictable alternative
  * to [[ExampleScenario]] for trying out the simulation.
  *
  * Nodes have no defense/patch and the malware has maximum infectivity, so the outbreak reliably
  * marches down the chain one hop at a time instead of depending on a coin flip per node. Nodes
  * also carry the same `NetworkExploit` vector as the malware (the DSL's default), so infection can
  * actually propagate down the chain: without a shared vector it would never spread past the seeded
  * starting node.
  */
object SimpleScenario:
  def linearScenario(): Either[String, Scenario] =
    val topologyResult: ValidationResult[Topology] =
      topology:
        node("n1", Workstation)
        node("n2", Workstation)
        node("n3", Workstation)
        node("n4", Workstation)
        "n1" <-> "n2" via ChannelType.LAN
        "n2" <-> "n3" via ChannelType.LAN
        "n3" <-> "n4" via ChannelType.LAN

    for
      topo <- topologyResult.left.map(_.mkString("; "))
      malware <- Malware(
        name = "SimpleWorm",
        kind = MalwareKind.Worm,
        traits = MalwareTraits(
          infectivity = Probability.clamped(1.0),
          stealth = Probability.clamped(0.3),
          payloadSeverity = PayloadSeverityLevel.Low,
          persistence = Probability.clamped(0.5),
          footprint = Probability.clamped(0.3)
        ),
        vectors = Set(PropagationVector.NetworkExploit)
      )
      scenario <- Scenario(
        name = "Linear chain",
        topology = topo,
        virus = malware,
        startingNode = topo.nodes("n1"),
        tick = 0,
        seed = 1,
        maxIterations = 20
      )
    yield scenario
