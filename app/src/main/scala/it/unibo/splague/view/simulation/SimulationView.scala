package it.unibo.splague.view.simulation

import it.unibo.splague.AppState
import it.unibo.splague.update.{Msg, TopologyShape}
import it.unibo.splague.view.simulation.dialog.ScenarioConfigDialog
import it.unibo.splague.view.simulation.workspace.ScenarioWorkspacePanel
import it.unibo.splague.view.form.ScenarioForm

import scala.swing.{
  BorderPanel,
  Button,
  ComboBox,
  Component,
  FlowPanel,
  Label,
  SplitPane,
  Window,
  Orientation as SwingOrientation
}
import scala.swing.event.ButtonClicked

object SimulationView:

  private final case class Session(
      workspace: ScenarioWorkspacePanel,
      configuration: ScenarioConfigDialog,
      tickLabel: Label,
      awarenessLabel: Label,
      runButton: Button,
      reportButton: Button,
      resetButton: Button,
      pauseToggleButton: Button,
      shapeButtons: Seq[Button],
      scenarioCombo: ComboBox[String],
      loadScenarioButton: Button,
      root: Component
  )

  private var currentSession: Option[Session] = None

  def render(
      state: AppState,
      owner: Window,
      dispatch: Msg => Unit
  ): Component =
    state.scenarioForm match
      case Some(form) =>
        // A simulation can be reset once it's either run to completion or been paused midway —
        // pausing must not be a dead end that only resuming-to-completion can escape.
        val canResetSimulation = state.simulation.exists(s => !s.running || s.paused)
        val simulationPaused = state.simulation.exists(_.paused)
        // The topology (including the shape-adding buttons and the scenario picker below) can
        // only be edited while no simulation is actively progressing; it's fine before one has
        // started, or once it's over.
        val simulationRunning = state.simulation.exists(_.running)
        // The dialog is blocked for as long as any simulation object exists at all — running,
        // paused, or finished-but-not-yet-reset — and only usable again once Reset clears it.
        val dialogInteractive = state.simulation.isEmpty
        // Reachable any time except mid-run — before a simulation exists, while paused, or once finished.
        val reportAccessible = state.simulation.forall(s => !s.running || s.paused)

        currentSession match
          case Some(session) =>
            session.update(
              state,
              form,
              reportAccessible,
              simulationRunning,
              simulationPaused,
              canResetSimulation,
              dialogInteractive
            )
            session.root
          case None =>
            val session = createSession(
              state,
              form,
              owner,
              reportAccessible,
              simulationRunning,
              simulationPaused,
              canResetSimulation,
              dialogInteractive,
              dispatch
            )
            currentSession = Some(session)
            session.root

      case None =>
        emptyView()

  def clearSession(): Unit =
    currentSession = None

  private def createSession(
      state: AppState,
      form: ScenarioForm,
      owner: Window,
      reportAccessible: Boolean,
      simulationRunning: Boolean,
      simulationPaused: Boolean,
      canResetSimulation: Boolean,
      dialogInteractive: Boolean,
      dispatch: Msg => Unit
  ): Session =
    val workspace =
      new ScenarioWorkspacePanel(
        initialTopology = form.topology,
        owner = owner,
        dispatch = dispatch
      )

    val configuration =
      new ScenarioConfigDialog(initialForm = form, dispatch = dispatch)
    configuration.setInteractive(dialogInteractive)

    val tickLabel = new Label(s"Tick: ${form.tick}")
    val awarenessLabel = new Label(formatAwareness(form.awareness)) // NEW

    // Disabled whenever a simulation already exists (running, paused, or finished-but-not-reset):
    // re-pressing Run in that state would treat the live/paused/final snapshot in scenarioForm as
    // a brand-new scenario, discarding the existing SimulationState and corrupting the pre-seed
    // model.currentScenario ResetSimulation depends on. Reset must happen first.
    val runButton = new Button("Run"):
      enabled = dialogInteractive

    runButton.listenTo(runButton)
    runButton.reactions += { case ButtonClicked(_) => dispatch(Msg.StartSimulation) }

    val reportButton = new Button("Report"):
      enabled = reportAccessible

    reportButton.listenTo(reportButton)
    reportButton.reactions += { case ButtonClicked(_) => dispatch(Msg.GoToReport) }

    // Enabled once the simulation has either run to completion or been paused, to bring the
    // workspace back to the state it was in when the simulation started.
    val resetButton = new Button("Reset"):
      enabled = canResetSimulation

    resetButton.listenTo(resetButton)
    resetButton.reactions += { case ButtonClicked(_) => dispatch(Msg.ResetSimulation) }

    // Pauses/resumes an already-started simulation; starting a new one is Run's job. Enabled
    // whenever there's a simulation with ticks left to give, regardless of paused state.
    val pauseToggleButton = new Button(pauseToggleLabel(simulationPaused)):
      enabled = simulationRunning

    pauseToggleButton.listenTo(pauseToggleButton)
    pauseToggleButton.reactions += { case ButtonClicked(_) =>
      dispatch(Msg.ToggleSimulationPause)
    }

    val shapeButtons = createShapeButtons(!simulationRunning, dispatch)
    val (scenarioCombo, loadScenarioButton) =
      createScenarioPicker(state, !simulationRunning, dispatch)

    val toolbar = createToolbar(
      workspace,
      tickLabel,
      awarenessLabel, // NEW
      runButton,
      reportButton,
      resetButton,
      pauseToggleButton,
      shapeButtons,
      scenarioCombo,
      loadScenarioButton,
      dispatch
    )

    val splitPane = new SplitPane(SwingOrientation.Vertical, workspace, configuration):
      oneTouchExpandable = true
      resizeWeight = 0.75

    splitPane.peer.setDividerLocation(620)

    val rootPanel = new BorderPanel:
      layout(toolbar) = BorderPanel.Position.North
      layout(splitPane) = BorderPanel.Position.Center

    Session(
      workspace = workspace,
      configuration = configuration,
      tickLabel = tickLabel,
      awarenessLabel = awarenessLabel, // NEW
      runButton = runButton,
      reportButton = reportButton,
      resetButton = resetButton,
      pauseToggleButton = pauseToggleButton,
      shapeButtons = shapeButtons,
      scenarioCombo = scenarioCombo,
      loadScenarioButton = loadScenarioButton,
      root = rootPanel
    )

  private def pauseToggleLabel(paused: Boolean): String =
    if paused then "Go" else "Stop"

  /** Formats a scenario's awareness level (a `Double` in `[0.0, 1.0]`) as a percentage for display,
    * e.g. `0.42` -> `"Awareness: 42%"`.
    */
  private def formatAwareness(value: Double): String = // NEW
    f"Awareness: ${value * 100}%.0f%%"

  /** A scenario picker ("switch to a different saved scenario") plus the "Load" button that applies
    * it (`Msg.SelectScenario`). Lists every scenario in `state.model.scenarios` — the two built-ins
    * (`SimpleScenario`, `ExampleScenario`) preloaded at startup, plus anything saved since
    * (`Mvu.saveScenario`) — and is re-synced with that list on every render via
    * `refreshScenarioItems`, since saving (possibly under a new name) can change it while this view
    * stays open. Disabled while the simulation is running, like the shape buttons: switching the
    * scenario out from under a live run would leave `SimulationState` pointing at a topology no
    * longer shown.
    */
  private def createScenarioPicker(
      state: AppState,
      enabledNow: Boolean,
      dispatch: Msg => Unit
  ): (ComboBox[String], Button) =
    val combo = new ComboBox(Seq.empty[String]):
      enabled = enabledNow

    refreshScenarioItems(combo, state)

    val load = new Button("Load"):
      enabled = enabledNow

    load.listenTo(load)
    load.reactions += { case ButtonClicked(_) =>
      Option(combo.peer.getSelectedItem)
        .map(_.toString)
        .foreach(name => dispatch(Msg.SelectScenario(name)))
    }

    (combo, load)

  /** Resets `combo` 's items to `state.model.scenarios` ' current names, keeping a sensible
    * selection: `state.model.currentScenario` 's name if it's in the list, else whatever was
    * selected before, if that's still in the list. Replaces the combo box's underlying model
    * outright rather than diffing it, since the list is short and this only runs on render.
    */
  private def refreshScenarioItems(combo: ComboBox[String], state: AppState): Unit =
    val names = state.model.scenarios.map(_.name)
    val previousSelection = Option(combo.peer.getSelectedItem).map(_.toString)

    combo.peer.setModel(new javax.swing.DefaultComboBoxModel[String](names.toArray))

    state.model.currentScenario
      .map(_.name)
      .filter(names.contains)
      .orElse(previousSelection.filter(names.contains))
      .foreach(name => combo.selection.item = name)

  /** One button per shape generator in `it.unibo.splague.dsl.TopologyShapes`, each adding that
    * shape to the workspace's topology (`Msg.AddShape`). Disabled while the simulation is running,
    * since the topology it's simulating shouldn't change underneath it.
    */
  private def createShapeButtons(enabledNow: Boolean, dispatch: Msg => Unit): Seq[Button] =
    Seq(
      "Star" -> TopologyShape.Star,
      "Ring" -> TopologyShape.Ring,
      "Mesh" -> TopologyShape.Mesh
    ).map { case (label, shape) =>
      val button = new Button(label):
        enabled = enabledNow

      button.listenTo(button)
      button.reactions += { case ButtonClicked(_) => dispatch(Msg.AddShape(shape)) }
      button
    }

  private def createToolbar(
      workspace: ScenarioWorkspacePanel,
      tickLabel: Label,
      awarenessLabel: Label, // NEW
      runButton: Button,
      reportButton: Button,
      resetButton: Button,
      pauseToggleButton: Button,
      shapeButtons: Seq[Button],
      scenarioCombo: ComboBox[String],
      loadScenarioButton: Button,
      dispatch: Msg => Unit
  ): Component =
    val zoomIn = new Button("+")
    zoomIn.listenTo(zoomIn)
    zoomIn.reactions += { case ButtonClicked(_) => workspace.zoomIn() }

    val zoomOut = new Button("-")
    zoomOut.listenTo(zoomOut)
    zoomOut.reactions += { case ButtonClicked(_) => workspace.zoomOut() }

    val back = new Button("Back")
    back.listenTo(back)
    back.reactions += { case ButtonClicked(_) =>
      clearSession()
      dispatch(Msg.GoToMenu)
    }

    val left = new FlowPanel(FlowPanel.Alignment.Left)(
      (Seq(zoomIn, zoomOut, scenarioCombo, loadScenarioButton) ++ shapeButtons ++ Seq(
        runButton,
        pauseToggleButton,
        resetButton,
        reportButton,
        tickLabel,
        awarenessLabel // NEW
      ))*
    ):
      hGap = 8
      vGap = 0

    val right = new FlowPanel(FlowPanel.Alignment.Right)(
      back,
      ExportButton(dispatch),
      ImportButton(dispatch)
    ):
      hGap = 8
      vGap = 0

    new BorderPanel:
      preferredSize = new scala.swing.Dimension(0, 52)
      layout(left) = BorderPanel.Position.West
      layout(right) = BorderPanel.Position.East

  private def emptyView(): Component =
    new BorderPanel:
      layout(new Label("No scenario form is open")) = BorderPanel.Position.Center

  extension (session: Session)
    private def update(
        state: AppState,
        form: ScenarioForm,
        reportAccessible: Boolean,
        simulationRunning: Boolean,
        simulationPaused: Boolean,
        canResetSimulation: Boolean,
        dialogInteractive: Boolean
    ): Unit =
      session.workspace.setTopology(form.topology)
      session.configuration.updateForm(form)
      session.configuration.setInteractive(dialogInteractive)
      session.tickLabel.text = s"Tick: ${form.tick}"
      session.awarenessLabel.text = formatAwareness(form.awareness) // NEW
      session.runButton.enabled = dialogInteractive
      session.reportButton.enabled = reportAccessible
      session.resetButton.enabled = canResetSimulation
      session.pauseToggleButton.text = pauseToggleLabel(simulationPaused)
      session.pauseToggleButton.enabled = simulationRunning
      session.shapeButtons.foreach(_.enabled = !simulationRunning)
      session.scenarioCombo.enabled = !simulationRunning
      session.loadScenarioButton.enabled = !simulationRunning
      refreshScenarioItems(session.scenarioCombo, state)
