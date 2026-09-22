package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.connection.Connection.ChannelType
import it.unibo.splague.model.connection.Protocol.ApplicationProtocolType
import it.unibo.splague.model.countermeasures.Countermeasures
import it.unibo.splague.model.malware.MalwareKind
import it.unibo.splague.model.malware.PayloadSeverityLevel
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.update.{FirewallPolicy, IsolationCriteria, Msg}
import it.unibo.splague.view.form.ScenarioForm

import javax.swing.{JSpinner, SpinnerNumberModel}
import scala.swing.{
  BorderPanel,
  BoxPanel,
  Button,
  CheckBox,
  ComboBox,
  Component,
  Dialog,
  FlowPanel,
  GridPanel,
  Label,
  Orientation,
  Panel,
  ScrollPane,
  TextField
}
import javax.swing.BorderFactory
import java.awt.Dimension

/** Form-only panel for editing the scenario-level fields and the malware configuration of a
  * [[ScenarioForm]]. It works exclusively with `ScenarioForm` / `MalwareForm` / `Msg`, never with
  * domain classes such as `Scenario` or `Malware`.
  *
  * Editable surface matches what the previous, dialog-based version actually exposed in its UI:
  * scenario name, seed, max iterations, starting node, and the malware fields. `tick`, `awareness`
  * and `countermeasureConfig` were never editable in the previous UI either (the latter was reset
  * to a hardcoded domain default on every save); to avoid inventing new UI or touching domain
  * classes, they are now carried through unchanged instead.
  */
final class ScenarioConfigDialog(
    initialForm: ScenarioForm,
    dispatch: Msg => Unit
) extends BorderPanel:

  private var currentForm: ScenarioForm = initialForm

  private val scenarioNameField = new TextField()

  private val seedSpinnerPeer =
    new JSpinner(new SpinnerNumberModel(0, 0, Int.MaxValue, 1))
  private val seedSpinner = Component.wrap(seedSpinnerPeer)

  private val maxIterSpinnerPeer =
    new JSpinner(new SpinnerNumberModel(1, 1, Int.MaxValue, 1))
  private val maxIterSpinner = Component.wrap(maxIterSpinnerPeer)

  private val startingNodeCombo = new ComboBox[String](Seq.empty)

  private val malwareNameField = new TextField()
  private val malwareKindCombo = new ComboBox[MalwareKind](MalwareKind.values.toSeq)

  private val infectivityField = new TextField()
  private val stealthField = new TextField()
  private val persistenceField = new TextField()
  private val footprintField = new TextField()

  private val payloadSeverityCombo =
    new ComboBox[PayloadSeverityLevel](PayloadSeverityLevel.values.toSeq)

  private val vectorChecks: Map[PropagationVector, CheckBox] =
    PropagationVector.values
      .map(vector => vector -> new CheckBox(vector.toString))
      .toMap

  // Countermeasures fields
  private val patchBoostField = new TextField()
  private val defenseBoostField = new TextField()
  private val patchCureProbField = new TextField()

  private val countermeasureRows: Map[Countermeasures, (CheckBox, TextField)] =
    Countermeasures.values.map { cm =>
      val check = new CheckBox(cm.toString)
      val field = new TextField {
        columns = 5
        enabled = false
      }

      check.reactions += { case scala.swing.event.ButtonClicked(_) =>
        field.enabled = check.selected
        if (!check.selected) field.text = ""
      }

      cm -> (check, field)
    }.toMap

  // Firewall Policy
  private val channelChecks: Map[ChannelType, CheckBox] =
    ChannelType.values.map(c => c -> new CheckBox(c.toString)).toMap

  private val protocolChecks: Map[ApplicationProtocolType, CheckBox] =
    ApplicationProtocolType.values.map(p => p -> new CheckBox(p.toString)).toMap

  // Isolation criteria
  private val isolationCombo = new ComboBox[String](
    Seq(
      "All",
      "By Min Workload",
      "By Max Defense"
      // TODO add by node type
    )
  )

  private val isolationThresholdField = new TextField {
    columns = 5
    enabled = false // Disabled by default because 'All' doesn't require threshold
  }

  // Enables/Disables the text field based on the chosen combobox
  isolationCombo.reactions += { case scala.swing.event.SelectionChanged(_) =>
    val requiresThreshold = isolationCombo.selection.item != "All"
    isolationThresholdField.enabled = requiresThreshold
    if (!requiresThreshold) isolationThresholdField.text = ""
  }

  private val formContent = new BoxPanel(Orientation.Vertical):
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

  border = BorderFactory.createEmptyBorder()

  formContent.contents += section("Scenario", scenarioFields())
  formContent.contents += section("Malware", malwareFields())
  formContent.contents += section("Countermeasures", countermeasureFields())

  private val scroll = new ScrollPane(formContent):
    border = BorderFactory.createEmptyBorder()
    peer.getVerticalScrollBar.setUnitIncrement(16)
    peer.getHorizontalScrollBar.setUnitIncrement(16)

  layout(scroll) = BorderPanel.Position.Center
  layout(buildButtons()) = BorderPanel.Position.South

  applyForm(initialForm)

  /** Re-syncs the panel with an updated [[ScenarioForm]] coming from the state, e.g. after a
    * topology change elsewhere in the workspace.
    */
  def updateForm(form: ScenarioForm): Unit =
    currentForm = form
    applyForm(form)

  private def applyForm(form: ScenarioForm): Unit =
    scenarioNameField.text = form.name

    form.seed.toIntOption.foreach(seedSpinnerPeer.setValue)
    form.maxIterations.toIntOption.foreach(maxIterSpinnerPeer.setValue)

    refreshStartingNodes(form)

    malwareNameField.text = form.virus.name
    malwareKindCombo.selection.item = form.virus.kind
    infectivityField.text = form.virus.infectivity
    stealthField.text = form.virus.stealth
    persistenceField.text = form.virus.persistence
    footprintField.text = form.virus.footprint
    payloadSeverityCombo.selection.item = form.virus.payloadSeverity

    vectorChecks.foreach { case (vector, checkbox) =>
      checkbox.selected = form.virus.vectors.contains(vector)
    }

  private def refreshStartingNodes(form: ScenarioForm): Unit =
    val previous = Option(startingNodeCombo.selection.item)
    val nodeIds = form.topology.nodes.map(_.id).sorted
    val availableIds = nodeIds.toSet

    // Create and set a mutable DefaultComboBoxModel
    val model = new javax.swing.DefaultComboBoxModel[String]()
    nodeIds.foreach(model.addElement)
    startingNodeCombo.peer.setModel(model)

    // Restore previous selection or set to first available
    previous
      .filter(availableIds.contains)
      .orElse(Some(form.startingNodeId).filter(availableIds.contains))
      .orElse(nodeIds.headOption)
      .foreach(id => startingNodeCombo.selection.item = id)

  private def scenarioFields(): Panel =
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
      contents += createLabeledField("Scenario name", scenarioNameField)
      contents += createLabeledField("Seed", seedSpinner)
      contents += createLabeledField("Max iterations", maxIterSpinner)
      contents += createLabeledField("Starting node", startingNodeCombo)

  private def malwareFields(): Panel =
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
      contents += createLabeledField("Malware name", malwareNameField)
      contents += createLabeledField("Kind", malwareKindCombo)
      contents += createLabeledField("Infectivity [0..1]", infectivityField)
      contents += createLabeledField("Stealth [0..1]", stealthField)
      contents += createLabeledField("Persistence [0..1]", persistenceField)
      contents += createLabeledField("Footprint [0..1]", footprintField)
      contents += createLabeledField("Payload severity", payloadSeverityCombo)
      contents += createLabeledField("Vectors", createVectorsPanel())

  // Countermeasure fields
  private def createCountermeasureLevelsPanel(): Panel =
    new BoxPanel(Orientation.Vertical):
      countermeasureRows.values.foreach { case (check, field) =>
        contents += new BoxPanel(Orientation.Horizontal):
          contents += check
          contents += scala.swing.Swing.HStrut(10)
          contents += new Label("Threshold:")
          contents += field

          maximumSize = new Dimension(Short.MaxValue, 30)
      }

  private def createFirewallPanel(): Panel =
    new BoxPanel(Orientation.Vertical):
      contents += new Label("Blocked Channels:")
      contents += new GridPanel(0, 2):
        hGap = 8;
        vGap = 4
        channelChecks.values.foreach(contents += _)

      contents += scala.swing.Swing.VStrut(8)

      contents += new Label("Blocked Protocols:")
      contents += new GridPanel(0, 2):
        hGap = 8;
        vGap = 4
        protocolChecks.values.foreach(contents += _)

  private def createIsolationPanel(): Panel =
    new BoxPanel(Orientation.Horizontal):
      contents += isolationCombo
      contents += scala.swing.Swing.HStrut(10)
      contents += new Label("Threshold:")
      contents += isolationThresholdField

  private def countermeasureFields(): Panel =
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createEmptyBorder(6, 6, 6, 6)
      contents += createCountermeasureLevelsPanel()
      contents += new scala.swing.Separator()
      contents += new Label("Firewall Policy")
      contents += createFirewallPanel()

      contents += new scala.swing.Separator()
      contents += new Label("Isolation Criteria")
      contents += createIsolationPanel()

  private def createLabeledField(labelText: String, field: Component): Panel =
    val label = new Label(labelText)
    label.preferredSize = new Dimension(120, 30)
    val fieldCopy = field
    fieldCopy.preferredSize = new Dimension(200, 30)
    new BoxPanel(Orientation.Horizontal):
      contents += label
      contents += fieldCopy

  private def createVectorsPanel(): Panel =
    new GridPanel(0, 2):
      hGap = 8
      vGap = 4
      vectorChecks.values.foreach { checkbox =>
        contents += checkbox
      }

  private def section(title: String, content: Panel): Panel =
    new BorderPanel:
      border = BorderFactory.createTitledBorder(title)
      layout(content) = BorderPanel.Position.Center

  private def buildButtons(): Panel =
    val save = new Button("Save scenario")
    val cancel = new Button("Cancel"):
      preferredSize = Dimension(100, 34)

    save.preferredSize = Dimension(120, 34)

    DialogUtils.setupButtonListeners(save, cancel, onSave, onCancel)

    val buttons = new FlowPanel(FlowPanel.Alignment.Right)(cancel, save):
      hGap = 10
      vGap = 6

    new BorderPanel:
      layout(buttons) = BorderPanel.Position.East

  private def onSave(): Unit =
    val updatedVectors =
      vectorChecks.collect { case (vector, checkbox) if checkbox.selected => vector }.toSet

    val updatedMalware = currentForm.virus.copy(
      name = malwareNameField.text.trim,
      kind = malwareKindCombo.selection.item,
      infectivity = infectivityField.text,
      stealth = stealthField.text,
      payloadSeverity = payloadSeverityCombo.selection.item,
      persistence = persistenceField.text,
      footprint = footprintField.text,
      vectors = updatedVectors
    )

    val updatedCountermeasure = currentForm.countermeasureConfig.copy(
      activeCountermeasures = buildActiveCountermeasures(),
      countermeasureLevels = buildCountermeasureLevels(),
      isolationCriteria = buildIsolationCriteria(),
      firewallPolicy = buildFirewallPolicy()
    )

    val updatedScenario = currentForm.copy(
      name = scenarioNameField.text.trim,
      seed = seedSpinnerPeer.getValue.toString,
      maxIterations = maxIterSpinnerPeer.getValue.toString,
      startingNodeId = Option(startingNodeCombo.peer.getSelectedItem)
        .map(_.toString)
        .getOrElse(currentForm.startingNodeId),
      virus = updatedMalware,
      countermeasureConfig = updatedCountermeasure
    )

    ScenarioForm.toDomain(updatedScenario) match
      case Left(error) =>
        Dialog.showMessage(this, error, title = "Errore", messageType = Dialog.Message.Error)
      case Right(_) =>
        currentForm = updatedScenario
        dispatch(Msg.UpdateScenarioName(updatedScenario))
        dispatch(Msg.UpdateMalware(updatedMalware))
        dispatch(Msg.UpdateCountermeasure(updatedCountermeasure))
        dispatch(Msg.SaveScenario)

  private def onCancel(): Unit =
    applyForm(currentForm)
    dispatch(Msg.CancelScenario)

  /** Extracts the set of active countermeasures selected by the user in the UI.
    *
    * @return
    *   a Set containing the selected [[Countermeasures]]
    */
  private def buildActiveCountermeasures(): Set[Countermeasures] =
    countermeasureRows.collect {
      case (cm, (check, _)) if check.selected => cm
    }.toSet

  /** Builds a mapping between threshold values (as strings) and their corresponding active
    * countermeasures, based on the user input in the enabled text fields.
    *
    * @return
    *   a Map associating the threshold string to the [[Countermeasures]]
    */
  private def buildCountermeasureLevels(): Map[String, Countermeasures] =
    countermeasureRows.collect {
      case (cm, (check, field)) if check.selected => field.text.trim -> cm
    }.toMap

  /** Constructs a new [[FirewallPolicy]] based on the selected channels and application protocols
    * from the UI checkboxes.
    *
    * @return
    *   a [[FirewallPolicy]] containing the blocked channels and protocols
    */
  private def buildFirewallPolicy(): FirewallPolicy =
    val blockedChannels = channelChecks.collect {
      case (ch, chk) if chk.selected => ch
    }.toSet
    val blockedProtocols = protocolChecks.collect {
      case (pr, chk) if chk.selected => pr
    }.toSet

    FirewallPolicy(blockedChannels, blockedProtocols)

  /** Creates an [[IsolationCriteria]] based on the selected strategy from the combo box and the
    * provided threshold value. If the threshold field cannot be parsed to a double, it defaults to
    * 0.0.
    *
    * @return
    *   the constructed [[IsolationCriteria]]
    */
  private def buildIsolationCriteria(): IsolationCriteria =
    val threshold = isolationThresholdField.text.trim.toDoubleOption.getOrElse(0.0)
    isolationCombo.selection.item match
      case "By Min Workload" => IsolationCriteria.byMinWorkload(threshold)
      case "By Max Defense"  => IsolationCriteria.byMaxDefense(threshold)
      case _                 => IsolationCriteria.all
