package it.unibo.splague.view

import it.unibo.splague.update.Mvu.Msg
import java.awt.{Dimension, Font}
import java.awt.event.{ComponentAdapter, ComponentEvent}
import scala.swing.{Action, Alignment, Button, Component, GridBagPanel, Label}

object SimulationView:
  def render(engine: Any, dispatch: Msg => Unit): Component =
    val panel = new GridBagPanel
    panel.preferredSize = new Dimension(800, 600)

    val title = centeredLabel(s"Simulation Running: $engine")
    val backButton = fullWidthButton("Back to menu") {
      dispatch(Msg.ReturnToMenu)
    }

    panel.peer.addComponentListener(new ComponentAdapter {
      override def componentResized(event: ComponentEvent): Unit =
        SimulationView.updateTextSizes(event.getSource.asInstanceOf[java.awt.Container])
    })

    val titleConstraints = new panel.Constraints:
      gridx = 0
      gridy = 0
      weightx = 1
      weighty = 1
      fill = GridBagPanel.Fill.Both

    val buttonConstraints = new panel.Constraints:
      gridx = 0
      gridy = 1
      weightx = 1
      weighty = 2
      fill = GridBagPanel.Fill.Both

    panel.layout(title) = titleConstraints
    panel.layout(backButton) = buttonConstraints

    SimulationView.updateTextSizes(panel.peer)
    panel

  private def centeredLabel(text: String): Label =
    new Label(text):
      horizontalAlignment = Alignment.Center
      verticalAlignment = Alignment.Center
      maximumSize = new Dimension(Short.MaxValue, Short.MaxValue)

  private def fullWidthButton(text: String)(action: => Unit): Button =
    new Button(Action(text) {
      action
    }):
      maximumSize = new Dimension(Short.MaxValue, Short.MaxValue)

  private def updateTextSizes(container: java.awt.Container): Unit =
    val height = math.max(1, container.getHeight)
    val children = container.getComponents

    val titleFontSize = math.max(20, height / 12)
    val buttonFontSize = math.max(16, height / 18)

    var i = 0
    while i < children.length do
      val child = children(i)
      if child != null then
        val fontSize = if i == 0 then titleFontSize else buttonFontSize
        child.setFont(new Font(Font.SANS_SERIF, Font.BOLD, fontSize))
      i += 1
