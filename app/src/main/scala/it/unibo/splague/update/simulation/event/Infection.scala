package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.node.{Node, NodeState, Topology}
import SimulationEvents.Event
import SimulationEvents.TopologyUpdateMixin
import it.unibo.splague.update.simulation.event.rules.ContagionRules

import scala.util.Random

object Infection:

  trait InfectionMixin:
    self: SimulationEvents.Event & TopologyUpdateMixin =>

    protected def infectedNodes(topology: Topology): Iterable[Node] =
      topology.nodes.values.filter(_.state == NodeState.Infected)

    protected def propagateFrom(
        topology: Topology,
        source: Node,
        malware: it.unibo.splague.model.malware.Malware,
        roll: Double
    ): Topology =
      val neighbors = topology.neighbors(source)

      neighbors.foldLeft(topology): (topoAcc, neighborFromEdge) =>
        topoAcc.nodes.get(neighborFromEdge.nodeId.value) match
          case Some(target)
              if target.state == NodeState.Healthy && ContagionRules.resolveInfection(
                malware,
                target,
                roll
              ) =>
            updateNode(topoAcc, neighborFromEdge.nodeId)(_.copy(state = NodeState.Infected))
          case _ =>
            topoAcc

  object InfectionEvent extends Event with TopologyUpdateMixin with InfectionMixin:

    override def apply(scenario: Scenario): Scenario =
      val rng = new Random(scenario.seed + scenario.tick)
      val topology = scenario.topology
      val malware = scenario.virus

      val newTopology =
        infectedNodes(topology).foldLeft(topology): (topoAcc, src) =>
          val roll = rng.nextDouble()
          propagateFrom(topoAcc, src, malware, roll)

      scenario.copy(topology = newTopology)
