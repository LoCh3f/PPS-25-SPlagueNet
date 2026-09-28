package it.unibo.splague.view.menu

import it.unibo.splague.update.Msg

import scala.swing.*

object MenuView:
  def render(dispatch: Msg => Unit): Component =
    val panel = new GridBagPanel
    panel.preferredSize = new Dimension(800, 600)

    val label = centeredLabel()
    val simulationButton = fullWidthButton("Go to Simulation") {
      dispatch(Msg.GoToSimulation)
    }
    val exitButton = fullWidthButton("Exit") {
      sys.exit(0)
    }

    val labelConstraints = new panel.Constraints:
      gridx = 0
      gridy = 0
      weightx = 1
      weighty = 1
      fill = GridBagPanel.Fill.Both

    val buttonConstraints = new panel.Constraints:
      gridx = 0
      weightx = 1
      fill = GridBagPanel.Fill.Both

    panel.layout(label) = labelConstraints

    buttonConstraints.gridy = 1
    buttonConstraints.weighty = 2
    panel.layout(simulationButton) = buttonConstraints

    buttonConstraints.gridy = 2
    buttonConstraints.weighty = 2
    panel.layout(exitButton) = buttonConstraints

    MenuView.updateTextSizes(panel.peer)
    panel

  private def centeredLabel(): Label =
    new Label("Welcome to SPlagueNet"):
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

    val labelFontSize = math.max(20, height / 12)
    val buttonFontSize = math.max(16, height / 18)

    var i = 0
    while i < children.length do
      val child = children(i)
      if child != null then
        val fontSize = if i == 0 then labelFontSize else buttonFontSize
        child.setFont(new java.awt.Font(java.awt.Font.SANS_SERIF, java.awt.Font.BOLD, fontSize))
      i += 1
