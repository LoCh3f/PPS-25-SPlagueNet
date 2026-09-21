package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.malware.MalwareKind
import it.unibo.splague.model.malware.PayloadSeverityLevel
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.ScenarioForm

import javax.swing.{JSpinner, SpinnerNumberModel}

import scala.swing.{
  BorderPanel,
  BoxPanel,
  Button,
  CheckBox,
  Component,
  ComboBox,
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

  private val formContent = new BoxPanel(Orientation.Vertical):
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)

  border = BorderFactory.createEmptyBorder()

  formContent.contents += section("Scenario", scenarioFields())
  formContent.contents += section("Malware", malwareFields())

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

    val updatedScenario = currentForm.copy(
      name = scenarioNameField.text.trim,
      seed = seedSpinnerPeer.getValue.toString,
      maxIterations = maxIterSpinnerPeer.getValue.toString,
      startingNodeId = Option(startingNodeCombo.peer.getSelectedItem)
        .map(_.toString)
        .getOrElse(currentForm.startingNodeId),
      virus = updatedMalware
    )

    ScenarioForm.toDomain(updatedScenario) match
      case Left(error) =>
        Dialog.showMessage(this, error, title = "Errore", messageType = Dialog.Message.Error)
      case Right(_) =>
        currentForm = updatedScenario
        dispatch(Msg.UpdateScenarioName(updatedScenario))
        dispatch(Msg.UpdateMalware(updatedMalware))
        dispatch(Msg.SaveScenario)

  private def onCancel(): Unit =
    applyForm(currentForm)
    dispatch(Msg.CancelScenario)
