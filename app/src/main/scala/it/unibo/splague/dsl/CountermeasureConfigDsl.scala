package it.unibo.splague.dsl
import it.unibo.splague.dsl.CountermeasureConfigBuilder
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}

/** Entry point for declaratively building a `CountermeasureConfig`.
  *
  * Usage:
  * {{{
  * val config = countermeasureConfig:
  *   active(Countermeasures.Patch, Countermeasures.Firewall)
  *   0.5 triggers Countermeasures.Isolation
  *   0.8 triggers Countermeasures.Shutdown
  *   patchBoostAmount(0.1)
  *   patchCureProbability(0.7)
  * }}}
  */
def countermeasureConfig(
    block: CountermeasureConfigBuilder ?=> Unit
): ValidationResult[CountermeasureConfig] =
  given builder: CountermeasureConfigBuilder = new CountermeasureConfigBuilder()
  block(using builder)
  builder.build()

def active(countermeasures: Countermeasures*)(using builder: CountermeasureConfigBuilder): Unit =
  builder.addActive(countermeasures*)

def patchBoostAmount(amount: Double)(using builder: CountermeasureConfigBuilder): Unit =
  builder.setPatchBoost(amount)

def defenseBoostAmount(amount: Double)(using builder: CountermeasureConfigBuilder): Unit =
  builder.setDefenseBoost(amount)

def patchCureProbability(prob: Double)(using builder: CountermeasureConfigBuilder): Unit =
  builder.setCureProbability(prob)

def isolationCriteria(criteria: IsolationCriteria)(using
    builder: CountermeasureConfigBuilder
): Unit =
  builder.setIsolationCriteria(criteria)

def firewallPolicy(policy: FirewallPolicy)(using builder: CountermeasureConfigBuilder): Unit =
  builder.setFirewallPolicy(policy)

extension (threshold: Double)
  infix def triggers(countermeasure: Countermeasures)(using
      builder: CountermeasureConfigBuilder
  ): Unit =
    builder.addLevel(threshold, countermeasure)
