package it.unibo.splague.view.form.countermeasure

case class IsolationForm(
    strategy: String, // "All", "By Min Workload", "By Max Defense", "By Type"
    threshold: String,
    nodeTypes: Set[String]
)
