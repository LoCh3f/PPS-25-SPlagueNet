package it.unibo.splague.view.simulation.dialog

import it.unibo.splague.model.malware.MalwareKind
import it.unibo.splague.model.malware.PayloadSeverityLevel
import it.unibo.splague.model.malware.PropagationVector
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.MalwareForm
import it.unibo.splague.view.form.ScenarioForm

import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.GridLayout
import java.awt.Insets
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JCheckBox
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSpinner
import javax.swing.JTextField
import javax.swing.SpinnerNumberModel

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
) extends JPanel(new BorderLayout(8, 8)):

  private var currentForm: ScenarioForm =
    initialForm

  private val scenarioNameField =
    new JTextField()

  private val seedSpinner =
    new JSpinner(
      new SpinnerNumberModel(0, 0, Int.MaxValue, 1)
    )

  private val maxIterSpinner =
    new JSpinner(
      new SpinnerNumberModel(1, 1, Int.MaxValue, 1)
    )

  private val startingNodeCombo =
    new JComboBox[String]()

  private val malwareNameField =
    new JTextField()

  private val malwareKindCombo =
    new JComboBox[MalwareKind](MalwareKind.values)

  private val infectivityField =
    new JTextField()

  private val stealthField =
    new JTextField()

  private val persistenceField =
    new JTextField()

  private val footprintField =
    new JTextField()

  private val payloadSeverityCombo =
    new JComboBox[PayloadSeverityLevel](PayloadSeverityLevel.values)

  private val vectorChecks: Map[PropagationVector, JCheckBox] =
    PropagationVector.values
      .map(vector => vector -> new JCheckBox(vector.toString))
      .toMap

  private val formContent =
    new JPanel(
      new GridLayout(0, 1, 10, 10)
    )

  setBorder(
    BorderFactory.createEmptyBorder()
  )

  formContent.setBorder(
    BorderFactory.createEmptyBorder(8, 8, 8, 8)
  )

  formContent.add(
    section("Scenario", scenarioFields())
  )

  formContent.add(
    section("Malware", malwareFields())
  )

  val scroll =
    new JScrollPane(formContent)

  scroll.setBorder(
    BorderFactory.createEmptyBorder()
  )

  scroll.getVerticalScrollBar.setUnitIncrement(16)
  scroll.getHorizontalScrollBar.setUnitIncrement(16)

  add(
    scroll,
    BorderLayout.CENTER
  )

  add(
    buildButtons(),
    BorderLayout.SOUTH
  )

  applyResponsiveSizing(formContent)
  applyForm(initialForm)

  /** Re-syncs the panel with an updated [[ScenarioForm]] coming from the state, e.g. after a
    * topology change elsewhere in the workspace.
    */
  def updateForm(
      form: ScenarioForm
  ): Unit =
    currentForm = form

    applyForm(form)

  private def applyForm(
      form: ScenarioForm
  ): Unit =
    scenarioNameField.setText(form.name)

    form.seed.toIntOption.foreach(
      seedSpinner.setValue
    )

    form.maxIterations.toIntOption.foreach(
      maxIterSpinner.setValue
    )

    refreshStartingNodes(form)

    malwareNameField.setText(form.virus.name)
    malwareKindCombo.setSelectedItem(form.virus.kind)
    infectivityField.setText(form.virus.infectivity)
    stealthField.setText(form.virus.stealth)
    persistenceField.setText(form.virus.persistence)
    footprintField.setText(form.virus.footprint)
    payloadSeverityCombo.setSelectedItem(form.virus.payloadSeverity)

    vectorChecks.foreach { case (vector, checkbox) =>
      checkbox.setSelected(
        form.virus.vectors.contains(vector)
      )
    }

  private def refreshStartingNodes(
      form: ScenarioForm
  ): Unit =
    val previous =
      Option(startingNodeCombo.getSelectedItem).map(_.toString)

    startingNodeCombo.removeAllItems()

    form.topology.nodes
      .map(_.id)
      .sorted
      .foreach(startingNodeCombo.addItem)

    val availableIds =
      form.topology.nodes.map(_.id).toSet

    previous
      .filter(availableIds.contains)
      .orElse(Some(form.startingNodeId).filter(availableIds.contains))
      .orElse(form.topology.nodes.map(_.id).sorted.headOption)
      .foreach(startingNodeCombo.setSelectedItem)

  private def scenarioFields(): JPanel =
    val panel =
      new JPanel(new GridBagLayout())

    panel.setBorder(
      BorderFactory.createEmptyBorder(6, 6, 6, 6)
    )

    val gbc =
      new GridBagConstraints()

    gbc.insets = new Insets(5, 5, 5, 5)
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.anchor = GridBagConstraints.LINE_START

    addField(panel, gbc, 0, "Scenario name", scenarioNameField)
    addField(panel, gbc, 1, "Seed", seedSpinner)
    addField(panel, gbc, 2, "Max iterations", maxIterSpinner)
    addField(panel, gbc, 3, "Starting node", startingNodeCombo)
    panel

  private def malwareFields(): JPanel =
    val panel =
      new JPanel(new GridBagLayout())

    panel.setBorder(
      BorderFactory.createEmptyBorder(6, 6, 6, 6)
    )

    val gbc =
      new GridBagConstraints()

    gbc.insets = new Insets(5, 5, 5, 5)
    gbc.fill = GridBagConstraints.HORIZONTAL
    gbc.anchor = GridBagConstraints.LINE_START

    addField(panel, gbc, 0, "Malware name", malwareNameField)
    addField(panel, gbc, 1, "Kind", malwareKindCombo)
    addField(panel, gbc, 2, "Infectivity [0..1]", infectivityField)
    addField(panel, gbc, 3, "Stealth [0..1]", stealthField)
    addField(panel, gbc, 4, "Persistence [0..1]", persistenceField)
    addField(panel, gbc, 5, "Footprint [0..1]", footprintField)
    addField(panel, gbc, 6, "Payload severity", payloadSeverityCombo)

    val label =
      new JLabel("Vectors")

    label.setFont(
      label.getFont.deriveFont(Font.BOLD)
    )

    gbc.gridx = 0
    gbc.gridy = 7
    gbc.weightx = 0.35
    gbc.gridwidth = 1
    panel.add(label, gbc)

    gbc.gridx = 1
    gbc.weightx = 0.65

    val vectorsPanel =
      new JPanel(new GridLayout(0, 2, 8, 4))

    vectorChecks.values.foreach { checkbox =>
      checkbox.setFont(
        checkbox.getFont.deriveFont(Font.PLAIN, 13f)
      )

      vectorsPanel.add(checkbox)
    }

    panel.add(vectorsPanel, gbc)
    panel

  private def section(
      title: String,
      content: JPanel
  ): JPanel =
    val wrapper =
      new JPanel(new BorderLayout(8, 8))

    wrapper.setBorder(
      BorderFactory.createTitledBorder(title)
    )

    wrapper.add(content, BorderLayout.CENTER)
    wrapper

  private def addField(
      panel: JPanel,
      gbc: GridBagConstraints,
      row: Int,
      labelText: String,
      field: JComponent
  ): Unit =
    val label =
      new JLabel(labelText)

    label.setFont(
      label.getFont.deriveFont(Font.BOLD)
    )

    gbc.gridx = 0
    gbc.gridy = row
    gbc.weightx = 0.35
    gbc.gridwidth = 1
    panel.add(label, gbc)

    gbc.gridx = 1
    gbc.weightx = 0.65
    field.setPreferredSize(new Dimension(120, 30))
    field.setMinimumSize(new Dimension(100, 28))
    panel.add(field, gbc)

  private def buildButtons(): JPanel =
    val panel =
      new JPanel(new BorderLayout())

    val save =
      new JButton("Save scenario")

    val cancel =
      new JButton("Cancel")

    save.addActionListener(_ => onSave())
    cancel.addActionListener(_ => onCancel())

    save.setPreferredSize(new Dimension(120, 34))
    cancel.setPreferredSize(new Dimension(100, 34))

    val buttons =
      new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6))

    buttons.add(save)
    buttons.add(cancel)
    panel.add(buttons, BorderLayout.EAST)
    panel

  private def onSave(): Unit =
    val updatedVectors =
      vectorChecks.collect {
        case (vector, checkbox) if checkbox.isSelected => vector
      }.toSet

    val updatedMalware =
      currentForm.virus.copy(
        name = malwareNameField.getText.trim,
        kind = malwareKindCombo.getSelectedItem.asInstanceOf[MalwareKind],
        infectivity = infectivityField.getText,
        stealth = stealthField.getText,
        payloadSeverity = payloadSeverityCombo.getSelectedItem.asInstanceOf[PayloadSeverityLevel],
        persistence = persistenceField.getText,
        footprint = footprintField.getText,
        vectors = updatedVectors
      )

    val updatedScenario =
      currentForm.copy(
        name = scenarioNameField.getText.trim,
        seed = seedSpinner.getValue.toString,
        maxIterations = maxIterSpinner.getValue.toString,
        startingNodeId = Option(startingNodeCombo.getSelectedItem)
          .map(_.toString)
          .getOrElse(currentForm.startingNodeId),
        virus = updatedMalware
      )

    ScenarioForm.toDomain(updatedScenario) match
      case Left(error) =>
        JOptionPane.showMessageDialog(
          this,
          error,
          "Errore",
          JOptionPane.ERROR_MESSAGE
        )

      case Right(_) =>
        currentForm = updatedScenario

        dispatch(
          Msg.UpdateScenarioName(updatedScenario)
        )

        dispatch(
          Msg.UpdateMalware(updatedMalware)
        )

        dispatch(
          Msg.SaveScenario
        )

  private def onCancel(): Unit =
    applyForm(currentForm)

    dispatch(
      Msg.CancelScenario
    )

  private def applyResponsiveSizing(
      container: java.awt.Container
  ): Unit =
    container.addComponentListener(
      new ComponentAdapter:
        override def componentResized(
            event: ComponentEvent
        ): Unit =
          val width =
            math.max(1, event.getComponent.getWidth)

          val base =
            responsiveFontSize(width)

          setFontSizeRecursively(container, base)
          resizeControls(container, width)
    )

    val width =
      math.max(1, container.getWidth)

    setFontSizeRecursively(container, responsiveFontSize(width))
    resizeControls(container, width)

  private def responsiveFontSize(
      width: Int
  ): Int =
    math.max(13, math.min(18, 13 + (width - 260) / 35))

  private def resizeControls(
      container: java.awt.Container,
      width: Int
  ): Unit =
    val baseHeight =
      math.max(26, math.min(34, 26 + (width - 220) / 18))

    setComponentSizeRecursively(container, baseHeight)

  private def setComponentSizeRecursively(
      component: java.awt.Component,
      baseHeight: Int
  ): Unit =
    component match
      case c: JComponent =>
        c match
          case field: JTextField =>
            field.setPreferredSize(
              new Dimension(math.max(120, c.getWidth), baseHeight)
            )

          case combo: JComboBox[?] =>
            combo.setPreferredSize(
              new Dimension(math.max(120, c.getWidth), baseHeight)
            )

          case spinner: JSpinner =>
            spinner.setPreferredSize(
              new Dimension(math.max(120, c.getWidth), baseHeight)
            )

          case button: JButton =>
            button.setPreferredSize(
              new Dimension(
                math.max(100, button.getWidth),
                math.max(30, baseHeight)
              )
            )

          case checkBox: JCheckBox =>
            checkBox.setPreferredSize(
              new Dimension(math.max(140, c.getWidth), baseHeight)
            )

          case _ =>
            ()

        c match
          case panel: JPanel =>
            panel.getComponents.foreach(child => setComponentSizeRecursively(child, baseHeight))

          case _ =>
            ()

      case _ =>
        ()

  private def setFontSizeRecursively(
      component: java.awt.Component,
      size: Int
  ): Unit =
    component match
      case c: JComponent =>
        c.setFont(
          c.getFont.deriveFont(size.toFloat)
        )

        c match
          case panel: JPanel =>
            panel.getComponents.foreach(child => setFontSizeRecursively(child, size))

          case _ =>
            ()

      case _ =>
        ()
