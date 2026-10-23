---
title: Paradisi Giovanni
nav_order: 1
parent: Implementazione
---

# Implementazione — Paradisi Giovanni

Le aree di cui sono responsabile principale sono: il modello del malware e le regole di
propagazione, la DSL interna per la dichiarazione di topologie e scenari, il sistema di
report e la sua persistenza, e i tipi opachi `Probability` e `Awareness`.

---

## Modello del malware e regole di propagazione

### Definizione del malware

Il malware è modellato come un tipo di dati immutabile con un costruttore *smart* che
forza il rispetto dell'invariante di dominio più importante: un malware deve dichiarare
almeno un vettore di propagazione.

```scala
object Malware:
  def apply(
      name: String,
      kind: MalwareKind,
      traits: MalwareTraits,
      vectors: Set[PropagationVector]
  ): Either[String, Malware] =
    Either.cond(
      vectors.nonEmpty,
      new Malware(name, kind, traits, vectors),
      "Malware must declare at least one propagation vector"
    )
```

Il costruttore primario è privato: fuori dal companion object non è possibile istanziare
un `Malware` con l'insieme dei vettori vuoto, eliminando alla radice una classe di errori
di runtime. Lo stesso pattern si ritrova in `Scenario`, `CountermeasureConfig` e `NodeId`.

I *tratti* del malware (`MalwareTraits`) raggruppano cinque valori di tipo `Probability`:
`infectivity`, `stealth`, `persistence`, `footprint` e `payloadSeverity`. L'uso del tipo
opaco `Probability` (descritto in seguito) rende impossibile passare un `Double` grezzo
dove è richiesto un valore validato.

### Pipeline di contagio

La probabilità di infezione di un nodo bersaglio non è un valore fisso: è il risultato
di una sequenza di modificatori applicati in cascata all'infectivity di base. Ogni
modificatore è una funzione pura del tipo:

```scala
private type Modifier = (Probability, Malware, Node, Edge) => Probability
```

I modificatori sono composti con `foldLeft`:

```scala
private val infectionPipeline: Seq[Modifier] = Seq(
  withDefense,
  withPatch,
  withStructuralVulnerability,
  withPacketLoss,
  withApplicationProtocol
)

def infectionProbability(malware: Malware, target: Node, edge: Edge): Probability =
  infectionPipeline.foldLeft(infectionBase(malware, target, edge)) { (acc, modifier) =>
    modifier(acc, malware, target, edge)
  }
```

Questo design mantiene ogni regola isolata e testabile individualmente, e permette di
aggiungere un nuovo fattore (ad esempio un modificatore legato al tipo di rete) senza
toccare il ciclo di composizione. Tutta la logica di contagio vive in `ContagionRules`,
separata dall'evento `Infection` che la invoca.

La scelta casuale è esternalizzata: `resolveInfection` riceve il valore `roll: Double`
già estratto dal chiamante, anziché generarlo internamente. In questo modo la funzione
rimane pura e il determinismo della simulazione è garantito dal seed dello scenario.

---

## `Probability` e `Awareness` come opaque types

Molti valori del dominio sono probabilità o livelli normalizzati in [0,1], così che un `Double` grezzo non possa essere passato dove è attesa una
probabilità già validata, e viceversa.

La soluzione adottata è l'**opaque type** di Scala 3:

```scala
opaque type Probability = Double

object Probability:
  def apply(value: Double): Either[String, Probability] =
    Either.cond(value >= 0.0 && value <= 1.0, value,
      s"Probability must be in [0,1], got $value")

  def clamped(value: Double): Probability = value.max(0.0).min(1.0)

  extension (p: Probability) def value: Double = p
```

Al di fuori del file di definizione, `Probability` è un tipo distinto da `Double`: il
compilatore rifiuta qualsiasi assegnazione diretta in entrambe le direzioni. All'interno
del file la rappresentazione è `Double` puro, quindi non vi è alcun overhead a runtime.
Lo stesso identico pattern è applicato ad `Awareness` e a `NodeId`.

`Awareness` aggiunge un comportamento specifico: il metodo `trackTowards` implementa
un aggiornamento di tipo EMA (*exponential moving average*) che fa convergere l'awareness
verso il segnale di rilevazione senza mai superarlo:

```scala
def trackTowards(signal: Double, rate: Double): Awareness =
  clamped(a + rate * (signal - a))
```

Il punto fisso di questa funzione è `signal` stesso: ripetendo l'aggiornamento all'infinito
l'awareness si stabilizza sul valore del segnale, non cresce indefinitamente. Questa
proprietà è verificata da un test di convergenza:

```scala
test("con un segnale costante, l'awareness converge vicino al segnale") {
  val result = (1 to 100).foldLeft(Awareness.none)(
    (a, _) => a.trackTowards(signal, rate.value)
  )
  result.value shouldBe signal +- 0.01
  result.value should be < 0.9
}
```

---

## DSL interna per topologie e scenari

### Motivazione e approccio

Lo scenario di simulazione è un oggetto relativamente complesso da costruire: richiede
una topologia valida, un malware, un nodo di partenza che sia parte della topologia, e
una configurazione delle contromisure. Esporre direttamente i costruttori avrebbe
richiesto agli utenti di comporre a mano oggetti annidati e gestire gli errori a ogni
livello.

La scelta è stata quella di progettare una **DSL interna** in Scala 3 che consenta di
dichiarare uno scenario in modo leggibile, accumuli tutti gli errori di validazione
invece di fermarsi al primo, e produca un `Either` come risultato. Il meccanismo
fondamentale che la rende possibile è la **context function**:

```scala
def topology(block: TopologyBuilder ?=> Unit): ValidationResult[Topology] =
  given builder: TopologyBuilder = new TopologyBuilder()
  block(using builder)
  builder.build()
```

Il parametro `block` è una funzione che richiede implicitamente un `TopologyBuilder`
nell'ambiente. Il chiamante non passa il builder esplicitamente: lo usa attraverso le
funzioni della DSL (`node`, `<->`, `via`) che lo ricevono come `using` parameter. Lo
stesso schema si ripete a ogni livello: `topology` dentro `network`, `network` dentro
`scenario`.

```scala
val result: ValidationResult[Scenario] = scenario("Outbreak"):
  network:
    node("A", Workstation)
    node("B", Server, defenseLevel = 0.3)
    "A" <-> "B" via LAN
  malware("Worm", Worm, infectivity = 0.8, ..., vectors = Set(NetworkExploit))
  startingNode("A")
  seed(42)
```

Il blocco `network` accetta lo stesso identico blocco di `topology`, quindi tutte le
validazioni della topologia (ID malformati, self-loop, archi duplicati, riferimenti a
nodi inesistenti) sono riusate senza duplicazioni.

### Accumulazione degli errori

Il tipo `ValidationResult[A] = Either[List[String], A]` è un alias di tipo che sostituisce
la convenzione `Either[String, A]` usata dai costruttori di dominio: a differenza di
questi ultimi, che segnalano un singolo errore, la DSL deve poter raccogliere tutti gli
errori di una dichiarazione nello stesso passaggio. L'helper `ValidationResult.partition`
separa i risultati in errori e successi in un'unica scansione della lista:

```scala
def partition[A](results: List[ValidationResult[A]]): (List[String], List[A]) =
  (results.collect { case Left(e) => e }.flatten,
   results.collect { case Right(a) => a })
```

I nodi e gli archi vengono validati indipendentemente: un ID malformato non impedisce
di rilevare anche un arco duplicato nello stesso blocco.

### Forme topologiche (`star`, `ring`, `mesh`)

Le tre forme sono generatori che costruiscono nodi e archi usando le primitive della
DSL stessa. Partecipano alla stessa `ValidationResult` accumulata del blocco che le
contiene: un prefisso duplicato produce errori di nodo duplicato, non un crash.

`ring` richiede due casi particolari: con un solo nodo produrrebbe un self-loop, con due
nodi produrrebbe un arco duplicato (dichiarato una volta per direzione). Entrambi sono
gestiti esplicitamente anziché lasciati emergere come errori di validazione, perché
l'intenzione dell'utente è costruire un ring valido, non ricevere un errore inaspettato:

```scala
count match
  case n if n <= 1 => ()          // nessun arco
  case 2           => "n0" <-> "n1" via channelType
  case n           => for i <- 0 until n do
                        s"n$i" <-> s"n${(i + 1) % n}" via channelType
```

`mesh` itera le coppie `(i, j)` con `i < j`, evitando sia self-loop che duplicati senza
casi speciali, perché la struttura dell'iterazione li esclude per costruzione.

---

## Scenario Report

### Struttura

Al termine di una simulazione il sistema produce un `ScenarioReport`, un valore
immutabile che riassume l'intera evoluzione:

- `timeline`: vettore di `TickSummary`, uno per tick, con il conteggio dei nodi per stato
  e il valore dell'awareness;
- `activationTicks`: mappa da contromisura al tick in cui si è attivata per la prima volta;
- `milestones`: primo tick di diffusione, picco di infezioni, primo tick con nodi distrutti.

La scelta progettuale più rilevante riguarda come produrre la timeline. Invece di
accumulare snapshot durante la simulazione live (che avrebbe richiesto di mantenere una
lista crescente in `SimulationState` a ogni tick), il report viene prodotto
**rieseguendo deterministicamente lo scenario iniziale**:

```scala
object ScenarioReport:
  def from(initial: Scenario, selector: EventSelector): ScenarioReport =
    val (timeline, activationTicks) =
      SimulationEngine(selector)
        .run(initial)
        .foldLeft((Vector.empty[TickSummary], Map.empty[Countermeasures, Int])) {
          case ((accTimeline, accActivation), scenario) =>
            val newlyActive =
              scenario.countermeasureConfig.activeCountermeasures -- accActivation.keySet
            (accTimeline :+ TickSummary.from(scenario),
             accActivation ++ newlyActive.map(_ -> scenario.tick))
        }
    ScenarioReport(initial.name, initial.seed, initial.virus.name,
                   timeline, activationTicks, Milestones.from(timeline))
```

`SimulationEngine.run` restituisce una `LazyList[Scenario]`: la sequenza è prodotta
pigramente e `foldLeft` la consuma in un singolo passaggio, costruendo timeline e mappa
delle attivazioni senza materializzare l'intera storia in memoria.

Questa scelta funziona perché tutta la logica di simulazione è pura: dato lo stesso
`initial` e lo stesso `selector`, `run` produce sempre la stessa sequenza. Il report
è quindi identico alla simulazione live senza richiedere stato condiviso.

### Schermata di report

`ReportView` è suddivisa in tre pannelli affiancati (`summaryPanel`):

- **Final node states**: conteggio finale per stato e awareness;
- **Countermeasures activated**: lista ordinata per tick di attivazione;
- **Key moments**: milestone con i tick notevoli.

I pulsanti *Save report* e *Import report* usano la stessa infrastruttura di persistenza
generica descritta sotto.

### Persistenza generica

Il sistema di persistenza è basato su un pattern **type class**. Le interfacce centrali
sono due type class parametriche su un formato file `F`:

```scala
trait Encoder[A, F <: FileFormat]:
  def encode(a: A): String

trait Decoder[A, F <: FileFormat]:
  def decode(raw: String): Either[PersistenceError, A]
```

Le implementazioni concretes per JSON delegano a Circe tramite istanze `given`:

```scala
given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json.type] with
  override def encode(a: A): String = cEncoder(a).noSpaces

given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json.type] with
  override def decode(raw: String): Either[PersistenceError, A] =
    io.circe.parser.decode[A](raw).left.map(e => PersistenceError.Parsing(e.getMessage))
```

`Repository[A]` combina `Writer[A]` e `Reader[A]` e viene costruito via factory method
che richiedono le istanze implicite dei codec:

```scala
def json[A](using
    enc: Encoder[A, FileFormat.Json.type],
    dec: Decoder[A, FileFormat.Json.type]
): Repository[A]
```

Questo significa che chiamare `Repository.json[ScenarioReport]` richiede al compilatore
di trovare nell'ambiente le istanze Circe per `ScenarioReport` e tutti i suoi campi:
se manca un codec (ad esempio per un nuovo tipo aggiunto al modello), il codice non
compila invece di fallire a runtime con un errore di serializzazione. Lo stesso
`Repository` è riusato per `Scenario` senza modifiche.

I `CodecCatalog` (uno per JSON, uno per TXT) centralizzano tutte le istanze `given` per
i tipi del dominio: tipi opachi (`NodeId`, `Probability`, `Awareness`), enumerazioni
(`NodeState`, `NodeType`, `ChannelType`), tipi con chiavi non stringa (`Map[Node, Double]`,
`Map[Double, Countermeasures]`) serializzati come liste di coppie per compatibilità con
Circe.