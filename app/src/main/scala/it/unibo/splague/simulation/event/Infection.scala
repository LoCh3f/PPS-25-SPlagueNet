package it.unibo.splague.simulation.event

import it.unibo.splague.model.node.{Node, NodeState, Topology}
import it.unibo.splague.simulation.Scenario
import it.unibo.splague.simulation.event.SimulationEvents.Event
import it.unibo.splague.simulation.event.SimulationEvents.TopologyUpdateMixin
import it.unibo.splague.update.ContagionRules

import scala.util.Random

object Infection:

  trait InfectionMixin:
    self: SimulationEvents.Event =>

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
        val idStr = neighborFromEdge.nodeId.value
        topoAcc.nodes.get(idStr) match
          case Some(target) if target.state == NodeState.Healthy =>
            if ContagionRules.resolveInfection(malware, target, roll) then
              val infectedTarget = target.copy(state = NodeState.Infected)
              topoAcc.copy(nodes = topoAcc.nodes.updated(idStr, infectedTarget))
            else topoAcc
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
