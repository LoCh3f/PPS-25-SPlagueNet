package it.unibo.splague.view.report

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.update.simulation.report.ScenarioReport

import scala.swing.{
  Alignment,
  BorderPanel,
  BoxPanel,
  Button,
  Component,
  FlowPanel,
  GridPanel,
  Label,
  Orientation
}
import scala.swing.event.ButtonClicked
import javax.swing.BorderFactory

object ReportView:

  def render(state: AppState, dispatch: Msg => Unit): Component =
    state.report match
      case Some(report) => reportPanel(report, dispatch)
      case None         => emptyView()

  private def reportPanel(report: ScenarioReport, dispatch: Msg => Unit): Component =
    val root = new BorderPanel:
      border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
      layout(headerPanel(report)) = BorderPanel.Position.North
      layout(summaryPanel(report)) = BorderPanel.Position.Center
      layout(footerPanel(dispatch)) = BorderPanel.Position.South

    root

  private def headerPanel(report: ScenarioReport): Component =
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Scenario")
      contents += new Label(s"Name: ${report.scenarioName}")
      contents += new Label(s"Seed: ${report.seed}")
      contents += new Label(s"Malware: ${report.malwareName}")
      contents += new Label(s"Ticks run: ${report.finalTick.tick}")

  private def summaryPanel(report: ScenarioReport): Component =
    new BoxPanel(Orientation.Horizontal):
      contents += finalStatePanel(report)
      contents += activationPanel(report)
      contents += milestonesPanel(report)

  private def finalStatePanel(report: ScenarioReport): Component =
    val finalTick = report.finalTick
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Final node states")
      contents += new GridPanel(3, 2):
        contents += new Label("Healthy")
        contents += new Label(finalTick.healthy.toString)
        contents += new Label("Infected")
        contents += new Label(finalTick.infected.toString)
        contents += new Label("Quarantined")
        contents += new Label(finalTick.quarantined.toString)
      contents += new GridPanel(3, 2):
        contents += new Label("Immune")
        contents += new Label(finalTick.immune.toString)
        contents += new Label("Destroyed")
        contents += new Label(finalTick.destroyed.toString)
        contents += new Label("Final awareness")
        contents += new Label(f"${finalTick.awareness}%.2f")

  private def activationPanel(report: ScenarioReport): Component =
    val sortedActivations = report.activationTicks.toVector.sortBy(_._2)
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Countermeasures activated")
      if sortedActivations.isEmpty then
        contents += new GridPanel(1, 2):
          contents += new Label("None activated")
          contents += new Label("")
      else
        val panels = sortedActivations.map { case (countermeasure, tick) =>
          new GridPanel(1, 2):
            contents += new Label(countermeasure.toString)
            contents += new Label(s"tick $tick")
        }
        contents ++= panels

  private def milestonesPanel(report: ScenarioReport): Component =
    val milestones = report.milestones
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Key moments")
      contents += new GridPanel(1, 2):
        contents += new Label("First spread")
        contents += new Label(milestones.firstSpreadTick.map(t => s"tick $t").getOrElse("never"))
      contents += new GridPanel(1, 2):
        contents += new Label("Peak infected")
        contents += new Label(
          s"${milestones.peakInfectedCount} at tick ${milestones.peakInfectedTick}"
        )
      contents += new GridPanel(1, 2):
        contents += new Label("First destruction")
        contents += new Label(
          milestones.firstDestructionTick.map(t => s"tick $t").getOrElse("never")
        )

  private def footerPanel(dispatch: Msg => Unit): Component =
    val backButton = new Button("Back to simulation")
    backButton.listenTo(backButton)
    backButton.reactions += { case ButtonClicked(_) =>
      dispatch(Msg.GoToSimulation)
    }

    new FlowPanel(FlowPanel.Alignment.Right)(backButton)

  private def emptyView(): Component =
    new BorderPanel:
      layout(
        new Label("No report available"):
          horizontalAlignment = Alignment.Center
      ) = BorderPanel.Position.Center
