package it.unibo.splague.view.report

import it.unibo.splague.AppState
import it.unibo.splague.update.Msg
import it.unibo.splague.update.simulation.report.ScenarioReport

import java.awt.{BorderLayout, FlowLayout, GridLayout}
import javax.swing.{BorderFactory, JButton, JLabel, JPanel, SwingConstants}
import scala.swing.Component

object ReportView:

  def render(state: AppState, dispatch: Msg => Unit): Component =
    state.report match
      case Some(report) => reportPanel(report, dispatch)
      case None         => emptyView()

  private def reportPanel(report: ScenarioReport, dispatch: Msg => Unit): Component =
    val root = new JPanel(new BorderLayout(8, 8))
    root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12))

    root.add(headerPanel(report), BorderLayout.NORTH)
    root.add(summaryPanel(report), BorderLayout.CENTER)
    root.add(footerPanel(dispatch), BorderLayout.SOUTH)

    Component.wrap(root)

  private def headerPanel(report: ScenarioReport): JPanel =
    val panel = new JPanel(new GridLayout(0, 1, 4, 4))
    panel.setBorder(BorderFactory.createTitledBorder("Scenario"))

    panel.add(new JLabel(s"Name: ${report.scenarioName}"))
    panel.add(new JLabel(s"Seed: ${report.seed}"))
    panel.add(new JLabel(s"Malware: ${report.malwareName}"))
    panel.add(new JLabel(s"Ticks run: ${report.finalTick.tick}"))
    panel

  private def summaryPanel(report: ScenarioReport): JPanel =
    val panel = new JPanel(new GridLayout(1, 2, 12, 0))
    panel.add(finalStatePanel(report))
    panel.add(activationPanel(report))
    panel

  private def finalStatePanel(report: ScenarioReport): JPanel =
    val panel = new JPanel(new GridLayout(0, 2, 4, 4))
    panel.setBorder(BorderFactory.createTitledBorder("Final node states"))

    val finalTick = report.finalTick

    panel.add(new JLabel("Healthy"))
    panel.add(new JLabel(finalTick.healthy.toString))
    panel.add(new JLabel("Infected"))
    panel.add(new JLabel(finalTick.infected.toString))
    panel.add(new JLabel("Quarantined"))
    panel.add(new JLabel(finalTick.quarantined.toString))
    panel.add(new JLabel("Immune"))
    panel.add(new JLabel(finalTick.immune.toString))
    panel.add(new JLabel("Destroyed"))
    panel.add(new JLabel(finalTick.destroyed.toString))
    panel.add(new JLabel("Final awareness"))
    panel.add(new JLabel(f"${finalTick.awareness}%.2f"))
    panel

  private def activationPanel(report: ScenarioReport): JPanel =
    val panel = new JPanel(new GridLayout(0, 2, 4, 4))
    panel.setBorder(BorderFactory.createTitledBorder("Countermeasures activated"))

    val sortedActivations = report.activationTicks.toVector.sortBy(_._2)

    if sortedActivations.isEmpty then
      panel.add(new JLabel("None activated"))
      panel.add(new JLabel(""))
    else
      sortedActivations.foreach { case (countermeasure, tick) =>
        panel.add(new JLabel(countermeasure.toString))
        panel.add(new JLabel(s"tick $tick"))
      }

    panel

  private def footerPanel(dispatch: Msg => Unit): JPanel =
    val panel = new JPanel(new FlowLayout(FlowLayout.RIGHT))

    val back = new JButton("Back to simulation")
    back.addActionListener(_ => dispatch(Msg.GoToSimulation))

    panel.add(back)
    panel

  private def emptyView(): Component =
    Component.wrap(
      new JPanel(new BorderLayout()) {
        add(new JLabel("No report available", SwingConstants.CENTER), BorderLayout.CENTER)
      }
    )
