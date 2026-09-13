package it.unibo.splague.update.simulation.event

import it.unibo.splague.model.Scenario
import it.unibo.splague.model.node.{Node, NodeState, Topology}
import SimulationEvents.Event
import SimulationEvents.TopologyUpdateMixin
import it.unibo.splague.update.simulation.event.rules.ContagionRules

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
      val edgesFromSource = topology.edgesOf(source)

      edgesFromSource.foldLeft(topology): (topoAcc, edge) =>
        val target = if edge.source.nodeId == source.nodeId then edge.target else edge.source
        val idStr = target.nodeId.value

        topoAcc.nodes.get(idStr) match
          case Some(currentTarget)
              if currentTarget.state == NodeState.Healthy && currentTarget.vectors.exists(
                malware.vectors
              ) =>
            if ContagionRules.resolveInfection(malware, currentTarget, edge, roll) then
              topoAcc.copy(nodes =
                topoAcc.nodes.updated(idStr, currentTarget.copy(state = NodeState.Infected))
              )
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
