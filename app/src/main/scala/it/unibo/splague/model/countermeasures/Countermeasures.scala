package it.unibo.splague.model.countermeasures

import it.unibo.splague.model.node.NodeType
import it.unibo.splague.model.node.NodeType.IoTDevice

enum Countermeasures:
  case DefenseBoost, Firewall, Isolation, Patch

  def isApplicableTo(nodeType: NodeType): Boolean = this match
    case Patch | DefenseBoost => nodeType != IoTDevice
    case _                    => true
