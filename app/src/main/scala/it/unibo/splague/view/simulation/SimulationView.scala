package it.unibo.splague.view.simulation

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.view.simulation.dialog.ScenarioConfigDialog
import it.unibo.splague.view.simulation.workspace.ScenarioWorkspacePanel
import it.unibo.splague.view.form.ScenarioForm

import scala.swing.{
  BorderPanel,
  Button,
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

        currentSession match
          case Some(session) =>
            session.update(form, simulationFinished)
            session.root
          case None =>
            val session = createSession(form, owner, simulationFinished, dispatch)
            currentSession = Some(session)
            session.root

      case None =>
        emptyView()

  def clearSession(): Unit =
    currentSession = None

  private def createSession(
      form: ScenarioForm,
      owner: Window,
      simulationFinished: Boolean,
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

    val toolbar = createToolbar(workspace, tickLabel, reportButton, resetButton, dispatch)

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
      root = rootPanel
    )

  private def createToolbar(
      workspace: ScenarioWorkspacePanel,
      tickLabel: Label,
      reportButton: Button,
      resetButton: Button,
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
      zoomIn,
      zoomOut,
      save,
      run,
      resetButton,
      reportButton,
      tickLabel
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
    private def update(form: ScenarioForm, simulationFinished: Boolean): Unit =
      session.workspace.setTopology(form.topology)
      session.configuration.updateForm(form)
      session.tickLabel.text = s"Tick: ${form.tick}"
      session.reportButton.enabled = simulationFinished
      session.resetButton.enabled = simulationFinished
