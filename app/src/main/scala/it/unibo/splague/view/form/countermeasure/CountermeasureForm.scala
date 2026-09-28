package it.unibo.splague.view.form.countermeasure

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.connection.Protocol.ApplicationProtocolType
import it.unibo.splague.model.countermeasures.{CountermeasureConfig, Countermeasures}
import it.unibo.splague.model.node.NodeType
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria}

final case class CountermeasureForm(
    activeCountermeasures: Set[String],
    countermeasureLevels: Map[String, String],
    patchBoostAmount: String,
    defenseBoostAmount: String,
    patchCureProbability: String,
    isolationCriteria: IsolationForm,
    firewallPolicy: FirewallForm
)

object CountermeasureForm:

  def allCountermeasureNames: Seq[String] = Seq("DefenseBoost", "Firewall", "Isolation", "Patch")
  def allChannelNames: Seq[String] = Seq("WAN", "LAN", "VPN")
  def allProtocolNames: Seq[String] = Seq("Telnet", "FTP", "HTTP")
  def allNodeTypeNames: Seq[String] = Seq("Workstation", "Server", "Router", "IoTDevice")

  def fromDomain(
      config: CountermeasureConfig
  ): CountermeasureForm =
    CountermeasureForm(
      activeCountermeasures = config.activeCountermeasures.map(_.toString),
      countermeasureLevels = config.countermeasureLevels.map { case (threshold, countermeasure) =>
        threshold.toString -> countermeasure.toString
      },
      patchBoostAmount = config.patchBoostAmount.toString,
      defenseBoostAmount = config.defenseBoostAmount.toString,
      patchCureProbability = config.patchCureProbability.toString,
      isolationCriteria = IsolationForm(
        strategy = "All",
        threshold = "",
        nodeTypes = Set.empty
      ),
      firewallPolicy = FirewallForm(
        blockedChannels = config.firewallPolicy.blockedChannels.map(_.toString),
        blockedProtocols = config.firewallPolicy.blockedApplicationProtocols.map(_.toString)
      )
    )

  def toDomain(
      form: CountermeasureForm
  ): Either[String, CountermeasureConfig] =
    for
      activeCMs <- parseSet(form.activeCountermeasures, parseCountermeasure)
      levels <- parseLevels(form.countermeasureLevels)
      patchBoost <- parseDouble(form.patchBoostAmount, "patchBoostAmount")
      defenseBoost <- parseDouble(form.defenseBoostAmount, "defenseBoostAmount")
      cureProbability <- parseDouble(form.patchCureProbability, "patchCureProbability")
      isolation <- parseIsolation(form.isolationCriteria)
      firewall <- parseFirewall(form.firewallPolicy)

      config <- CountermeasureConfig(
        activeCountermeasures = activeCMs,
        countermeasureLevels = levels,
        patchBoostAmount = patchBoost,
        defenseBoostAmount = defenseBoost,
        patchCureProbability = cureProbability,
        isolationCriteria = isolation,
        firewallPolicy = firewall
      )
    yield config

  private def parseLevels(
      levels: Map[String, String]
  ): Either[String, Map[Double, Countermeasures]] =
    levels.foldLeft(
      Right(Map.empty): Either[String, Map[Double, Countermeasures]]
    ) { case (result, (rawThreshold, rawCountermeasure)) =>
      for
        parsedMap <- result
        threshold <- parseDouble(rawThreshold, s"countermeasure threshold '$rawThreshold'")
        countermeasure <- parseCountermeasure(rawCountermeasure)
      yield parsedMap.updated(threshold, countermeasure)
    }

  private def parseIsolation(form: IsolationForm): Either[String, IsolationCriteria] =
    form.strategy match
      case "By Min Workload" =>
        parseDouble(form.threshold, "Isolation threshold (Workload)").map(
          IsolationCriteria.byMinWorkload
        )
      case "By Max Defense" =>
        parseDouble(form.threshold, "Isolation threshold (Defense)").map(
          IsolationCriteria.byMaxDefense
        )
      case "By Type" =>
        parseSet(form.nodeTypes, parseNodeType).map(IsolationCriteria.byType)
      case _ => Right(IsolationCriteria.all)

  private def parseFirewall(form: FirewallForm): Either[String, FirewallPolicy] =
    for
      channels <- parseSet(form.blockedChannels, parseChannel)
      protocols <- parseSet(form.blockedProtocols, parseProtocol)
    yield FirewallPolicy(blockedChannels = channels, blockedApplicationProtocols = protocols)

  // Parsing utilities
  private def parseDouble(rawValue: String, field: String): Either[String, Double] =
    rawValue.trim.toDoubleOption.toRight(s"$field must be a valid number, got '$rawValue'")

  private def parseSet[A](
      rawSet: Set[String],
      parser: String => Either[String, A]
  ): Either[String, Set[A]] =
    rawSet.foldLeft(Right(Set.empty): Either[String, Set[A]]) { (result, raw) =>
      for
        parsedSet <- result
        parsedElement <- parser(raw)
      yield parsedSet + parsedElement
    }

  // Parsing methods for domain entities
  private def parseCountermeasure(s: String): Either[String, Countermeasures] =
    Countermeasures.values.find(_.toString == s).toRight(s"Unknown countermeasure: $s")

  private def parseNodeType(s: String): Either[String, NodeType] =
    s match
      case "Workstation" => Right(NodeType.Workstation)
      case "Server"      => Right(NodeType.Server)
      case "Router"      => Right(NodeType.Router)
      case "IoTDevice"   => Right(NodeType.IoTDevice)
      case _             => Left(s"Unknown Node Type: $s")

  private def parseChannel(s: String): Either[String, ChannelType] =
    ChannelType.values.find(_.toString == s).toRight(s"Unknown Channel Type: $s")

  private def parseProtocol(s: String): Either[String, ApplicationProtocolType] =
    ApplicationProtocolType.values.find(_.toString == s).toRight(s"Unknown Protocol: $s")
