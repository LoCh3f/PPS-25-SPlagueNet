package it.unibo.splague.utils

import it.unibo.splague.model.Probability
import it.unibo.splague.model.Scenario
import it.unibo.splague.model.connection.Connection.{Channel, ChannelType, Edge}
import it.unibo.splague.model.malware.{
  Malware,
  MalwareKind,
  MalwareTraits,
  PayloadSeverityLevel,
  PropagationVector
}
import it.unibo.splague.model.node.NodeState.Healthy
import it.unibo.splague.model.node.NodeType.Workstation
import it.unibo.splague.model.node.{Node, NodeId, Topology}

/** A minimal, easy-to-follow scenario: four workstations chained in a straight line (`n1 -> n2 ->
  * n3 -> n4`), with `n1` as the outbreak's starting node. Meant as a small, predictable alternative
  * to [[ExampleScenario]] for trying out the simulation.
  *
  * Nodes have no defense/patch and the malware has maximum infectivity, so the outbreak reliably
  * marches down the chain one hop at a time instead of depending on a coin flip per node. Nodes
  * also carry the same `NetworkExploit` vector as the malware: infection only ever propagates to a
  * node that shares at least one propagation vector with the malware, so without this the chain
  * would never actually spread past the seeded starting node.
  */
object SimpleScenario:
  def linearScenario(): Either[String, Scenario] =
    val ids: Vector[NodeId.NodeId] =
      Vector("n1", "n2", "n3", "n4").map(NodeId.of(_).toOption.get)

    val nodes: Map[String, Node] =
      ids.map { id =>
        id.value -> Node(
          id,
          Workstation,
          patchLevel = 0.0,
          defenseLevel = 0.0,
          state = Healthy,
          workload = 0.0,
          vectors = Set(PropagationVector.NetworkExploit)
        )
      }.toMap

    val edges: Set[Edge] =
      ids
        .sliding(2)
        .collect { case Vector(from, to) =>
          Edge(
            nodes(from.value),
            nodes(to.value),
            Channel.default(ChannelType.LAN),
            protocol = None
          )
        }
        .toSet

    val topology = Topology(nodes, edges)

    for
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
        topology = topology,
        virus = malware,
        startingNode = nodes(ids.head.value),
        tick = 0,
        seed = 1,
        maxIterations = 20
      )
    yield scenario
