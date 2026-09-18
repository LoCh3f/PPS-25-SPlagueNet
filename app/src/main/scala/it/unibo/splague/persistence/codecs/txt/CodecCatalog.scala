package it.unibo.splague.persistence.codecs.txt

import it.unibo.splague.model.Scenario
import it.unibo.splague.persistence.FileFormat
import it.unibo.splague.persistence.codecs.Encoder

object CodecCatalog {

  // Scenario encoder in txt
  given Encoder[Scenario, FileFormat.Txt.type] with
    override def encode(scenario: Scenario): String =
      val nodes = scenario.topology.nodes.values
        .map(n =>
          s"  - ID: ${n.nodeId.value}, Type: ${n.nodeType}, State: ${n.state}, Workload: ${n.workload}"
        )
        .mkString("\n")

      val edges = scenario.topology.edges
        .map(e =>
          s"  - Src: ${e.source.nodeId.value} -> Target: ${e.target.nodeId.value} | Channel: ${e.channel.channelType} (BW: ${e.channel.bandwidth}, Latency: ${e.channel.latency}) | Protocol: ${e.protocol.getOrElse("None")}"
        )
        .mkString("\n")

      s"""=== PLAGUENET SCENARIO EXPORT ===
         |Name: ${scenario.name}
         |Tick: ${scenario.tick}
         |Seed: ${scenario.seed}
         |Max Iterations: ${scenario.maxIterations}
         |Awareness: ${scenario.awareness}
         |Starting node: ${scenario.startingNode.nodeId.value}
         |-----------------------------------
         |NODES:
         |$nodes
         |-----------------------------------
         |EDGES:
         |$edges
         |-----------------------------------
         |""".stripMargin
}
