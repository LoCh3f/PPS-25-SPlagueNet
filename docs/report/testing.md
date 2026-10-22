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

| Metrica | Valore | Metrica | Valore | Metrica | Valore | Metrica | Valore |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Lines of code:** | 2349 | **Files:** | 45 | **Classes:** | 74 | **Methods:** | 291 |
| **Lines per file:** | 52.20 | **Packages:** | 16 | **Classes per package:** | 4.62 | **Methods per class:** | 3.93 |
| **Total statements:** | 1105 | **Invoked statements:** | 1041 | **Total branches:** | 106 | **Invoked branches:** | 96 |
| **Ignored statements:** | 0 | | | | | | |
| **Statement coverage:** | 94.21 % | | | **Branch coverage:** | 90.57 % | | |

Una vista più dettagliata, contenente la coverage per ogni singolo package:

| All packages | 94.21% |
| :--- | ---: |
| it.unibo.splague.dsl | 98.39% |
| it.unibo.splague.model | 100.00% |
| it.unibo.splague.model.connection | 100.00% |
| it.unibo.splague.model.countermeasures | 95.92% |
| it.unibo.splague.model.malware | 100.00% |
| it.unibo.splague.model.node | 93.13% |
| it.unibo.splague.model.report | 100.00% |
| it.unibo.splague.persistence | 81.58% |
| it.unibo.splague.persistence.codecs.json | 100.00% |
| it.unibo.splague.persistence.codecs.txt | 100.00% |
| it.unibo.splague.update | 100.00% |
| it.unibo.splague.update.simulation | 100.00% |
| it.unibo.splague.update.simulation.event | 91.41% |
| it.unibo.splague.update.simulation.event.rules | 96.00% |
| it.unibo.splague.update.simulation.report | 100.00% |

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