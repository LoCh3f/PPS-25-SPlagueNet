---
title: Testing
nav_order: 7
parent: Report
---
# Testing

## Tecnologie utilizzate

I test sono scritti con **ScalaTest** (versione 3.2.19), stile `AnyFunSuite with Matchers`,
con `EitherValues` per i risultati di tipo `Either`. Ogni suite è annotata con
`@RunWith(classOf[JUnitRunner])` per essere eseguita dal runner JUnit 4 integrato in Gradle.

La misurazione della copertura è affidata a **Scoverage** (plugin Gradle 9.1). Il report
viene generato con `./gradlew reportScoverage` e caricato come artefatto dalla pipeline CI.
L'analisi statica con **SpotBugs** è parte del task `check`: i warning sono trattati come
errori di build.

---

## Approccio e evoluzione della strategia

Il progetto adotta **TDD (Test-Driven Development)** come metodologia principale: ogni componente è stato sviluppato
scrivendo prima il test che ne esprimeva il comportamento atteso, poi l'implementazione
minima sufficiente a farlo passare.

Inizialmente i test verificavano dettagli interni molto specifici (tramite metodi ausiliari). Questa pratica è stata presto superata per concentrarsi esclusivamente sulle **API pubbliche**: in questo modo i test non risentono dei cambiamenti di implementazione e rimangono stabili durante i refactoring.

Nel layer MVU (`Mvu.update`), ad esempio, si è scelto di verificare i **cambiamenti di stato complessivi**
prodotti da ogni messaggio, piuttosto che le singole operazioni interne. Questo ha reso i test
robusti rispetto ai refactoring e ha evidenziato invarianti di dominio non sempre ovvi, come
il fatto che `ResetSimulation` debba correttamente ripristinare lo scenario pre-seed e non quello già infetto.

---

## Copertura

Il package `it.unibo.splague.view.*` è escluso dal calcolo perché le classi Swing non sono
eseguibili in ambiente headless. Tutto il resto — dominio, DSL, motore di simulazione,
persistenza, layer MVU — è coperto dai test automatici.

| Package | Copertura |
|---------|-----------|
| `model` | alta |
| `model.report` | alta |
| `dsl` | alta |
| `update.simulation.event` | alta |
| `update.simulation.report` | alta |
| `update.simulation` | alta |
| `update` | alta |
| `persistence` | alta |
| `view.form` | parziale |
| `view.*` | escluso |

---

## Esempi rilevanti

### Accumulazione degli errori nella DSL

La DSL raccoglie tutti gli errori di una dichiarazione invece di fermarsi al primo.
Il test verifica che problemi indipendenti — un ID malformato e un riferimento a nodo
inesistente — vengano riportati insieme nello stesso `Left`:

```scala
test("errors from nodes and edges accumulate together in the same result"):
  val result = topology:
    node("bad id", Workstation)
    node("A", Workstation)
    "bad id" <-> "ghost" via LAN

  val errors = result.left.value
  errors should contain("The ID cannot contain white space")
  errors should contain("Edge references unknown node id: ghost")
```

### Convergenza dell'awareness

Verifica che `trackTowards` abbia il segnale come punto fisso, e non accumuli
linearmente verso 1.0:

```scala
test("con un segnale costante, l'awareness converge vicino al segnale"):
  val result = (1 to 100).foldLeft(Awareness.none)(
    (a, _) => a.trackTowards(signal, rate.value)
  )
  result.value shouldBe signal +- 0.01
  result.value should be < 0.9
```

### Invariante del reset

Verifica che `ResetSimulation` ripristini lo scenario pre-seed da `model.currentScenario`,
non lo scenario già infetto conservato in `simulation.initial`. Questo distingue
l'*identità* dello scenario dalla sua *istanza in esecuzione*:

```scala
test("ResetSimulation restores model.currentScenario, which stays Healthy"):
  val result = Mvu.update(Msg.ResetSimulation, finished)

  result.simulation shouldBe None
  result.model.currentScenario shouldBe Some(baseline)
  result.scenarioForm.get.topology.nodes.map(_.state) shouldBe
    Vector(NodeState.Healthy)
```

---

## Integrazione continua

`./gradlew check` esegue in sequenza: compilazione, test, verifica formattazione scalafmt
e analisi SpotBugs. Il fallimento di uno qualsiasi blocca il merge della Pull Request.
Il report Scoverage è generato separatamente con `./gradlew reportScoverage` per non
rallentare il feedback loop principale del check.