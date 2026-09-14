package it.unibo.splague.view.simulation

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.view.form.{ScenarioForm, TopologyForm}

import java.awt.{BorderLayout, Dimension, FlowLayout}
import javax.swing.{BorderFactory, JButton, JLabel, JPanel, JSplitPane, JToolBar, SwingConstants}
import scala.swing.Component

object SimulationView:

  private final case class Session(
      workspace: ScenarioWorkspacePanel,
      configuration: ScenarioConfigDialog,
      tickLabel: JLabel,
      root: Component
  )

  private var currentSession: Option[Session] = None

  def render(
      state: AppState,
      dispatch: Msg => Unit
  ): Component =
    state.scenarioForm match
      case Some(form) =>
        currentSession match
          case Some(session) =>
            session.update(form)
            session.root

          case None =>
            val session = createSession(form, dispatch)
            currentSession = Some(session)
            session.root

      case None =>
        emptyView()

  def clearSession(): Unit =
    currentSession = None

  private def createSession(
      form: ScenarioForm,
      dispatch: Msg => Unit
  ): Session =
    val workspace =
      new ScenarioWorkspacePanel(
        initialTopology = form.topology,
        dispatch = dispatch
      )

    val configuration =
      new ScenarioConfigDialog(
        initialForm = form,
        dispatch = dispatch
      )

    val tickLabel =
      new JLabel(s"Tick: ${form.tick}")

    val toolbar =
      createToolbar(workspace, tickLabel, dispatch)

    val splitPane =
      new JSplitPane(
        JSplitPane.HORIZONTAL_SPLIT,
        workspace,
        configuration
      )

    splitPane.setOneTouchExpandable(true)
    splitPane.setResizeWeight(0.75)
    splitPane.setDividerLocation(620)
    splitPane.setBorder(BorderFactory.createEmptyBorder())

    val rootPanel =
      new JPanel(new BorderLayout())

    rootPanel.add(toolbar, BorderLayout.NORTH)
    rootPanel.add(splitPane, BorderLayout.CENTER)

    Session(
      workspace = workspace,
      configuration = configuration,
      tickLabel = tickLabel,
      root = Component.wrap(rootPanel)
    )

  private def createToolbar(
      workspace: ScenarioWorkspacePanel,
      tickLabel: JLabel,
      dispatch: Msg => Unit
  ): JToolBar =
    val toolbar = new JToolBar
    toolbar.setFloatable(false)
    toolbar.setPreferredSize(new Dimension(0, 52))

    val left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0))
    val right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0))

    val zoomIn = new JButton("+")
    zoomIn.addActionListener(_ => workspace.zoomIn())

    val zoomOut = new JButton("-")
    zoomOut.addActionListener(_ => workspace.zoomOut())

    val save = new JButton("Save")
    save.addActionListener(_ => dispatch(Msg.SaveScenario))

    val run = new JButton("Run")
    run.addActionListener(_ => dispatch(Msg.StartSimulation))

    val step = new JButton("Step")
    step.addActionListener(_ => dispatch(Msg.SimulationStep))

    val back = new JButton("Back")
    back.addActionListener(_ =>
      clearSession()
      dispatch(Msg.GoToMenu)
    )

    left.add(zoomIn)
    left.add(zoomOut)
    left.add(save)
    left.add(run)
    left.add(step)
    left.add(tickLabel)
    right.add(back)

    toolbar.setLayout(new BorderLayout())
    toolbar.add(left, BorderLayout.WEST)
    toolbar.add(right, BorderLayout.EAST)
    toolbar

  private def emptyView(): Component =
    Component.wrap(
      new JPanel(new BorderLayout()) {
        add(
          new JLabel(
            "No scenario form is open",
            SwingConstants.CENTER
          ),
          BorderLayout.CENTER
        )
      }
    )

  extension (session: Session)
    private def update(form: ScenarioForm): Unit =
      session.workspace.setTopology(form.topology)
      session.configuration.updateForm(form)
      session.tickLabel.setText(s"Tick: ${form.tick}")
