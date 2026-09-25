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
      reportButton: Button,
      resetButton: Button,
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
        // The simulation is over: the "final" state can be inspected in a report, and the
        // workspace can be reset back to the state the simulation started from.
        val simulationFinished = state.simulation.exists(!_.running)
        // The topology (including the shape-adding buttons and the scenario picker below) can
        // only be edited while no simulation is actively progressing; it's fine before one has
        // started, or once it's over.
        val simulationRunning = state.simulation.exists(_.running)

        currentSession match
          case Some(session) =>
            session.update(state, form, simulationFinished, simulationRunning)
            session.root
          case None =>
            val session =
              createSession(state, form, owner, simulationFinished, simulationRunning, dispatch)
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
      simulationFinished: Boolean,
      simulationRunning: Boolean,
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

    val tickLabel = new Label(s"Tick: ${form.tick}")

    val reportButton = new Button("Report"):
      enabled = simulationFinished

    reportButton.listenTo(reportButton)
    reportButton.reactions += { case ButtonClicked(_) => dispatch(Msg.GoToReport) }

    // Only meaningful once the simulation has run to completion, to bring the workspace back to
    // the state it was in when the simulation started.
    val resetButton = new Button("Reset"):
      enabled = simulationFinished

    resetButton.listenTo(resetButton)
    resetButton.reactions += { case ButtonClicked(_) => dispatch(Msg.ResetSimulation) }

    val shapeButtons = createShapeButtons(!simulationRunning, dispatch)
    val (scenarioCombo, loadScenarioButton) =
      createScenarioPicker(state, !simulationRunning, dispatch)

    val toolbar = createToolbar(
      workspace,
      tickLabel,
      reportButton,
      resetButton,
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
      reportButton = reportButton,
      resetButton = resetButton,
      shapeButtons = shapeButtons,
      scenarioCombo = scenarioCombo,
      loadScenarioButton = loadScenarioButton,
      root = rootPanel
    )

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
      reportButton: Button,
      resetButton: Button,
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

    val save = new Button("Save")
    save.listenTo(save)
    save.reactions += { case ButtonClicked(_) => dispatch(Msg.SaveScenario) }

    val run = new Button("Run")
    run.listenTo(run)
    run.reactions += { case ButtonClicked(_) => dispatch(Msg.StartSimulation) }

    val back = new Button("Back")
    back.listenTo(back)
    back.reactions += { case ButtonClicked(_) =>
      clearSession()
      dispatch(Msg.GoToMenu)
    }

    val left = new FlowPanel(FlowPanel.Alignment.Left)(
      (Seq(zoomIn, zoomOut, scenarioCombo, loadScenarioButton) ++ shapeButtons ++ Seq(
        save,
        run,
        resetButton,
        reportButton,
        tickLabel
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
        simulationFinished: Boolean,
        simulationRunning: Boolean
    ): Unit =
      session.workspace.setTopology(form.topology)
      session.configuration.updateForm(form)
      session.tickLabel.text = s"Tick: ${form.tick}"
      session.reportButton.enabled = simulationFinished
      session.resetButton.enabled = simulationFinished
      session.shapeButtons.foreach(_.enabled = !simulationRunning)
      session.scenarioCombo.enabled = !simulationRunning
      session.loadScenarioButton.enabled = !simulationRunning
      refreshScenarioItems(session.scenarioCombo, state)
