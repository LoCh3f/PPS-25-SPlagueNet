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
import it.unibo.splague.persistence.{ExportPaths, Repository}
import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
import it.unibo.splague.persistence.codecs.json.JsonCodec.given
import javax.swing.{JFileChooser, JOptionPane}
import java.awt.Component as AwtComponent
import javax.swing.SwingUtilities

object ReportView:

  private val reportRepo = Repository.json[ScenarioReport]

  private def saveReport(report: ScenarioReport, owner: AwtComponent): Unit =
    val path = ExportPaths.reportPathFor(report.scenarioName)
    reportRepo.save(report, path) match
      case Right(_) =>
        JOptionPane.showMessageDialog(
          SwingUtilities.getWindowAncestor(owner),
          s"Report saved to $path"
        )
      case Left(error) =>
        JOptionPane.showMessageDialog(
          SwingUtilities.getWindowAncestor(owner),
          error.toString,
          "Save failed",
          JOptionPane.ERROR_MESSAGE
        )

  private def importReport(dispatch: Msg => Unit, owner: AwtComponent): Unit =
    val chooser = new JFileChooser(ExportPaths.baseDirectory.toFile)
    if chooser.showOpenDialog(owner) == JFileChooser.APPROVE_OPTION then
      reportRepo.load(chooser.getSelectedFile.toPath) match
        case Right(report) => dispatch(Msg.ImportReport(report))
        case Left(error) =>
          JOptionPane.showMessageDialog(
            owner,
            error.toString,
            "Import failed",
            JOptionPane.ERROR_MESSAGE
          )

  def render(state: AppState, dispatch: Msg => Unit): Component =
    state.report match
      case Some(report) => reportPanel(report, dispatch)
      case None         => emptyView(dispatch)

  private def reportPanel(report: ScenarioReport, dispatch: Msg => Unit): Component =
    val root = new BorderPanel:
      border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
      layout(headerPanel(report)) = BorderPanel.Position.North
      layout(summaryPanel(report)) = BorderPanel.Position.Center
      layout(footerPanel(report, dispatch)) = BorderPanel.Position.South

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

  private def footerPanel(report: ScenarioReport, dispatch: Msg => Unit): Component =
    val backButton = new Button("Back to simulation")
    backButton.listenTo(backButton)
    backButton.reactions += { case ButtonClicked(_) =>
      dispatch(Msg.GoToSimulation)
    }
    val saveButton = new Button("Save report")
    saveButton.listenTo(saveButton)
    saveButton.reactions += { case ButtonClicked(_) => saveReport(report, saveButton.peer) }

    val importButton = new Button("Import report")
    importButton.listenTo(importButton)
    importButton.reactions += { case ButtonClicked(_) => importReport(dispatch, importButton.peer) }

    new FlowPanel(FlowPanel.Alignment.Right)(importButton, saveButton, backButton)

  private def emptyView(dispatch: Msg => Unit): Component =
    new BorderPanel:
      layout(
        new Label("No report available"):
          horizontalAlignment = Alignment.Center
      ) = BorderPanel.Position.Center
      layout(emptyFooterPanel(dispatch)) = BorderPanel.Position.South

  private def emptyFooterPanel(dispatch: Msg => Unit): Component =
    val backButton = new Button("Back to simulation")
    backButton.listenTo(backButton)
    backButton.reactions += { case ButtonClicked(_) => dispatch(Msg.GoToSimulation) }

    val importButton = new Button("Import report")
    importButton.listenTo(importButton)
    importButton.reactions += { case ButtonClicked(_) => importReport(dispatch, importButton.peer) }

    new FlowPanel(FlowPanel.Alignment.Right)(importButton, backButton)
