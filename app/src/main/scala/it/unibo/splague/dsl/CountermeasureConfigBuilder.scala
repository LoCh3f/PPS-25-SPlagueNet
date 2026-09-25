package it.unibo.splague.dsl

import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}

import scala.collection.mutable

private final class CountermeasureConfigBuilder:

  private val activeSet = mutable.Set.empty[Countermeasures]
  private val levels = mutable.Map.empty[Double, Countermeasures]

  // defaults
  private var pBoost: Double = CountermeasureConfig.defaultPatchBoostAmount
  private var dBoost: Double = CountermeasureConfig.defaultDefenseBoostAmount
  private var pCureProb: Double = CountermeasureConfig.defaultPatchCureProbability
  private var iCriteria: IsolationCriteria = CountermeasureConfig.defaultIsolationCriteria
  private var fPolicy: FirewallPolicy = CountermeasureConfig.defaultFirewallPolicy

  def addActive(cms: Countermeasures*): Unit =
    activeSet ++= cms

  def addLevel(value: Double, cm: Countermeasures): Unit =
    levels += (value -> cm)

  def setPatchBoost(amount: Double): Unit =
    pBoost = amount

  def setDefenseBoost(amount: Double): Unit =
    dBoost = amount

  def setCureProbability(amount: Double): Unit =
    pCureProb = amount

  def setIsolationCriteria(criteria: IsolationCriteria): Unit =
    iCriteria = criteria

  def setFirewallPolicy(policy: FirewallPolicy): Unit =
    fPolicy = policy

  def build(): ValidationResult[CountermeasureConfig] =
    CountermeasureConfig(
      activeCountermeasures = activeSet.toSet,
      countermeasureLevels = levels.toMap,
      patchBoostAmount = pBoost,
      defenseBoostAmount = dBoost,
      patchCureProbability = pCureProb,
      isolationCriteria = iCriteria,
      firewallPolicy = fPolicy
    ).left.map(err => List(err))
