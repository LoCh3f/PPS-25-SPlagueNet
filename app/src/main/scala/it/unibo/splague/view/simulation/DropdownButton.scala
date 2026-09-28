package it.unibo.splague.view.simulation

import scala.swing.{Button, Component, MenuItem, PopupMenu}
import scala.swing.event.ButtonClicked

/** Shared dropdown-button behavior used by scenario import and export actions. */
object DropdownButton:

  def apply(
      label: String,
      items: Seq[(String, () => Unit)]
  ): Component =
    val button = new Button(label):
      focusable = false

    val menu = new PopupMenu
    items.foreach { case (itemLabel, action) =>
      val item = new MenuItem(itemLabel)
      item.listenTo(item)
      item.reactions += { case ButtonClicked(_) => action() }
      menu.contents += item
    }

    button.listenTo(button)
    button.reactions += { case ButtonClicked(_) =>
      menu.show(button, 0, button.bounds.height)
    }

    button
