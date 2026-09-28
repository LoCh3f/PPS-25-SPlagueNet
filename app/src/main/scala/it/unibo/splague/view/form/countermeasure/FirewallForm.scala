package it.unibo.splague.view.form.countermeasure

case class FirewallForm(
    blockedChannels: Set[String],
    blockedProtocols: Set[String]
)
