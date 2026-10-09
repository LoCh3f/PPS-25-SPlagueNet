package it.unibo.splague.view.report

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.update.simulation.report.ScenarioReport
import it.unibo.splague.view.ViewHelpers.*

import scala.swing.*
import scala.swing.event.ButtonClicked
import javax.swing.{BorderFactory, JFileChooser, JOptionPane}
import java.awt.Component as AwtComponent
import javax.swing.SwingUtilities
import it.unibo.splague.persistence.{ExportPaths, Repository}
import it.unibo.splague.persistence.codecs.json.CodecCatalog.given
import it.unibo.splague.persistence.codecs.json.JsonCodec.given

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
    new BorderPanel:
      border = BorderFactory.createEmptyBorder(12, 12, 12, 12)
      layout(headerPanel(report)) = BorderPanel.Position.North
      layout(summaryPanel(report)) = BorderPanel.Position.Center
      layout(footerPanel(report, dispatch)) = BorderPanel.Position.South

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
    val t = report.finalTick
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Final node states")
      contents += labelValueRow("Healthy", t.healthy.toString)
      contents += labelValueRow("Infected", t.infected.toString)
      contents += labelValueRow("Quarantined", t.quarantined.toString)
      contents += labelValueRow("Immune", t.immune.toString)
      contents += labelValueRow("Destroyed", t.destroyed.toString)
      contents += labelValueRow("Final awareness", f"${t.awareness}%.2f")

  private def activationPanel(report: ScenarioReport): Component =
    val sorted = report.activationTicks.toVector.sortBy(_._2)
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Countermeasures activated")
      if sorted.isEmpty then contents += labelValueRow("None activated", "")
      else
        sorted.foreach { case (cm, tick) =>
          contents += labelValueRow(cm.toString, s"tick $tick")
        }

  private def milestonesPanel(report: ScenarioReport): Component =
    val m = report.milestones
    new BoxPanel(Orientation.Vertical):
      border = BorderFactory.createTitledBorder("Key moments")
      contents += labelValueRow(
        "First spread",
        m.firstSpreadTick.map(t => s"tick $t").getOrElse("never")
      )
      contents += labelValueRow(
        "Peak infected",
        s"${m.peakInfectedCount} at tick ${m.peakInfectedTick}"
      )
      contents += labelValueRow(
        "First destruction",
        m.firstDestructionTick.map(t => s"tick $t").getOrElse("never")
      )

  // footer buttons — saveButton keeps its peer for the dialog owner, so stays manual
  private def footerPanel(report: ScenarioReport, dispatch: Msg => Unit): Component =
    val saveButton = new Button("Save report")
    saveButton.listenTo(saveButton)
    saveButton.reactions += { case ButtonClicked(_) => saveReport(report, saveButton.peer) }

    new FlowPanel(FlowPanel.Alignment.Right)(
      importButton(dispatch),
      saveButton,
      backButton(dispatch)
    )

  private def backButton(dispatch: Msg => Unit): Button =
    actionButton("Back to simulation") { dispatch(Msg.GoToSimulation) }

  private def importButton(dispatch: Msg => Unit): Button =
    val b = new Button("Import report")
    b.listenTo(b)
    b.reactions += { case ButtonClicked(_) => importReport(dispatch, b.peer) }
    b

  private def emptyView(dispatch: Msg => Unit): Component =
    new BorderPanel:
      layout(
        new Label("No report available"):
          horizontalAlignment = Alignment.Center
      ) = BorderPanel.Position.Center
      layout(emptyFooterPanel(dispatch)) = BorderPanel.Position.South

  private def emptyFooterPanel(dispatch: Msg => Unit): Component =
    new FlowPanel(FlowPanel.Alignment.Right)(importButton(dispatch), backButton(dispatch))
