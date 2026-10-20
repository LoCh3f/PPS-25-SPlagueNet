---
title: Implementazione — Componenti di Dominio, DSL e Persistenza
nav_order: 1
parent: Implementazione
---

# Implementazione — Componenti di Dominio, DSL e Persistenza

Le aree coperte in questa sezione includono: i modelli core (`Node`, `NodeId`, `NodeType`, `Scenario`, `CountermeasureConfig`),
la gestione sicura degli identificatori tramite tipi opachi, la definizione di eventi e contromisure,
la DSL per la configurazione delle contromisure, e il sistema generico di persistenza (tramite `Repository`, `Codec` e Type Classes).

---

## Modello di dominio e Smart Constructors

### Costruzione sicura delle entità

Il cuore del dominio (`Node`, `Scenario`, `CountermeasureConfig`) è modellato tramite *case class* immutabili. Per garantire la validità dello stato fin dal momento dell'istanziazione, il costruttore primario è reso `private`
a favore di **smart constructors** definiti nei rispettivi *companion objects*.

Ogni metodo `apply` convalida i parametri di ingresso e restituisce un `Either[String, A]`:

```scala
object Node:
  def apply(
      nodeId: NodeId,
      nodeType: NodeType = defaultNodeType,
      patchLevel: Double = defaultPatchLevel,
      // ...
  ): Either[String, Node] =
    for
      validPatchLevel <- fieldIsInRange(patchLevel, (0.0, 1.0))
      validDefenseLevel <- fieldIsInRange(defenseLevel, (0.0, 1.0))
    yield new Node(nodeId, nodeType, validPatchLevel, validDefenseLevel, state, workload, vectors)
```
Questo pattern di Fail-Fast garantisce che non possano esistere a runtime istanze con probabilità fuori dal range [0, 1]
o scenari con un nodo di partenza inesistente.
Lo stesso meccanismo si applica a Scenario e CountermeasureConfig.

### Tassonomia dei Nodi e dei Comportamenti
I tipi di nodo sono modellati tramite un sealed trait `NodeType` i cui oggetti definiscono parametri strutturali intrinseci:

```Scala
sealed trait NodeType:
    def detectionCoefficient: Double
    def structuralVulnerability: Double
```
Questa struttura permette di calcolare la vulnerabilità non come semplice dato di base,
ma differenziando l'efficacia difensiva a seconda che il target sia, ad esempio, un `Server` (dotato di SIEM, logging)
oppure un `IoTDevice` (vulnerabilità del firmware più elevata e scarse capacità di detection).

### `NodeId` come Opaque Type
Per prevenire errori di scambio di variabili (ad esempio, passare una banale stringa di log al posto dell'identificativo di un nodo), l'ID del nodo è implementato usando un opaque type di Scala 3:

```Scala
object NodeId:
  opaque type NodeId = String

  def normalize(rawId: String): String = rawId.trim()

  def of(rawId: String): Either[String, NodeId] =
    val normalized = normalize(rawId)
    if normalized.isEmpty then Left("The ID cannot be empty")
    else if normalized.exists(_.isWhitespace) then Left("The ID cannot contain white space")
    else Right(normalized)
```
Il tipo nasconde la sua rappresentazione interna (String) fuori dallo scope in cui è definito. L'unico modo per ottenere
un NodeId è passare per la validazione (`of`), la quale impone regole stringenti (nessun whitespace, stringa non vuota)
tramite normalizzazione. Questo elimina id errati e impedisce al compilatore di 
mescolare identificatori validati con stringhe grezze.

### Topology
L'analisi dei grafi è disaccoppiata dalla class `Topology` usando gli extension methods all'interno del companion object.
Questo lascia `Topology` come un mero contenitore di `Map[String, Node]` e `Set[Edge]`, mentre la logica di esplorazione
è modulare.

Un esempio chiave è l'algoritmo BFS impiegato per stabilire il set di nodi raggiungibili:

```scala
def reachableFrom(node: Node): Set[Node] =
    val initialNeighbors: Set[Node] = neighbors(node)
    val queue = mutable.Queue[Node]()
    val visited = mutable.Set[Node](node)

    queue.enqueueAll(initialNeighbors)
    visited.addAll(initialNeighbors)

    while queue.nonEmpty do
        val curr = queue.dequeue()
        for nextNeighbor <- neighbors(curr) do
        if !visited.contains(nextNeighbor) then
            visited.add(nextNeighbor)
            queue.enqueue(nextNeighbor)

    visited.toSet - node
```
L'algoritmo impiega `mutable.Queue` e `mutable.Set` strettamente confinati nello scope locale del metodo per ottenere
performance migliori rispetto a una visita puramente funzionale, mantenendo però la trasparenza referenziale all'esterno
(ritorna un set di nodi immutabile).

## Sistema di Persistenza Generico e Codec Catalog

### Il Problema
In un sistema di simulazione complesso, la serializzazione dello stato e dei report pone diverse sfide architetturali:
1. **Disaccoppiamento**: Legare direttamente le classi di dominio a una specifica libreria di serializzazione (es. annotazioni di libreria direttamente nei modelli) "inquina" il dominio e rende difficile cambiare tecnologia in futuro.
2. **Asimmetria delle operazioni**: Alcuni formati (come il JSON) devono supportare sia il salvataggio che il caricamento dello stato (lettura/scrittura), mentre altri formati (come un report TXT human-readable) hanno senso solo come esportazione (sola scrittura).
3. **Complessità dei tipi**: Il dominio fa largo uso di costrutti avanzati di Scala 3 (Opaque Types, Enums) e strutture dati non banali (mappe con chiavi complesse non-stringa) che i serializzatori standard non sanno gestire nativamente, causando potenziali errori a runtime.

### La Soluzione: Segregazione delle Interfacce e Type Classes
Per risolvere questi problemi, il modulo di persistenza è stato progettato attorno a un'architettura basata sul pattern **Type Class** e sul *Interface Segregation Principle* (ISP).

Invece di un'unica interfaccia monolitica, le operazioni di I/O sono state separate:

```scala
trait Writer[A]:
  def save(a: A, path: Path): Either[PersistenceError, Path]

trait Reader[A]:
  def load(path: Path): Either[PersistenceError, A]

trait Repository[A] extends Writer[A] with Reader[A]
object Repository:
    def json[A](using
                enc: Encoder[A, FileFormat.Json.type],
                dec: Decoder[A, FileFormat.Json.type]
               ): Repository[A] =
      new Repository[A]:
    
    def save(a: A, path: Path): Either[PersistenceError, Path] =
      Try(Files.writeString(path, enc.encode(a))).toEither.left
        .map(e => PersistenceError.IO(e.getMessage))
        .map(_ => path)
    
    def load(path: Path): Either[PersistenceError, A] =
      for
        raw <- Try(Files.readString(path)).toEither.left.map(e =>
          PersistenceError.IO(e.getMessage)
        )
        a <- dec.decode(raw)
      yield a
    
    def txtWriter[A](using enc: Encoder[A, FileFormat.Txt.type]): Writer[A] =
      new Writer[A]:
    def save(a: A, path: Path): Either[PersistenceError, Path] =
      Try(Files.writeString(path, enc.encode(a))).toEither.left
        .map(e => PersistenceError.IO(e.getMessage))
        .map(_ => path)
```
Questa distinzione ci permette di esprimere esattamente le reali capacità richieste dai diversi formati:

- **JSON (`Repository.json`)**: Richiede sia lettura che scrittura. Viene istanziato un `Repository` completo, permettendo di salvare e ripristinare interamente uno `Scenario` o un `ScenarioReport`.
- **TXT (`Repository.txtWriter`)**: Richiede solo la scrittura. Il sistema espone unicamente un `Writer`, poiché non vi è alcuna necessità (né logica) di effettuare il parsing inverso di un file di log testuale e destrutturato.

### Encoder e Decoder
Il motore della serializzazione non è implementato direttamente in Repository, ma è delegato a due interfacce puramente funzionali parametrizzate sul tipo di formato (`FileFormat`):

```scala
trait Encoder[A, F <: FileFormat]:
  def encode(a: A): String

trait Decoder[A, F <: FileFormat]:
  def decode(raw: String): Either[PersistenceError, A]
```
Questo design rende l'encoding e il decoding totalmente liberi e flessibili. Il framework sa solo che per salvare un 
tipo `A` in un formato `F` ha bisogno della corrispondente _given_ instance nell'ambiente.
Non impone alcuna restrizione su _come_ la stringa finale venga prodotta o analizzata.

### CodecCatalog
Il punto focale dell'architettura si concretizza nei cataloghi dei codec, i quali definiscono le regole di traduzione specifiche per ogni formato, isolando completamente le librerie esterne.

#### Il Catalogo JSON (Integrazione con Circe)
Per la rappresentazione strutturata abbiamo scelto di basarci sulla libreria `Circe`. Tuttavia, Circe non "invade" il
resto del sistema. Nel file `JsonCodec`, abbiamo creato un bridge che mappa i nostri `Encoder`/`Decoder` astratti sulle interfacce di Circe (`CirceEncoder/CirceDecoder`).

```scala
object JsonCodec {

  given [A](using cEncoder: CirceEncoder[A]): Encoder[A, FileFormat.Json.type] with
    override def encode(a: A): String = cEncoder(a).noSpaces

  given [A](using cDecoder: CirceDecoder[A]): Decoder[A, FileFormat.Json.type] with
    override def decode(raw: String): Either[PersistenceError, A] =
      io.circe.parser.decode[A](raw) match
        case Right(value) => Right(value)
        case Left(error)  => Left(PersistenceError.Parsing(error.getMessage))

}
```

Nel file CodecCatalog per il JSON vengono definiti tutti i mapping specifici per i tipi di dominio. Poiché Circe non supporta nativamente gli `Opaque Types` o chiavi complesse, il catalogo istruisce il compilatore su come trattarli:

```scala
// Mappatura sicura per gli Opaque Type delegando la validazione (Fail-Fast in lettura)
given nodeIdCirceDecoder: CirceDecoder[NodeId] =
  CirceDecoder.decodeString.emap { raw =>
    NodeId.of(raw) match
      case Right(id) => Right(id)
      case Left(err) => Left(err)
}

// Mappatura per chiavi complesse non-stringa trasformando le mappe in liste di tuple
given mapDoubleCountermeasuresEncoder: CirceEncoder[Map[Double, Countermeasures]] =
  CirceEncoder.encodeList[(Double, Countermeasures)].contramap(_.toList)
```
Questo garantisce che, per qualsiasi aggiunta futura al dominio, basterà aggiungere le regole nel catalogo JSON;
se una regola manca, il sistema impedirà la compilazione del Repository, garantendo totale sicurezza.

#### Il Catalogo TXT
A differenza del JSON, la rappresentazione Txt non usa alcuna libreria di terze parti. Essendo un formato orientato
all'esportazione leggibile dall'utente, l'implementazione in CodecCatalog sfrutta la completa flessibilità dell'interfaccia
`Encoder` per costruire manualmente le stringhe tramite interpolazione, estraendo solo i dati necessari e formattandoli
come se fossero una "fotografia" dello stato attuale:

```scala
given Encoder[Scenario, FileFormat.Txt.type] with
  override def encode(scenario: Scenario): String =
    val nodes = scenario.topology.nodes.values
      .map(n => s"  - ID: ${n.nodeId.value}, Type: ${n.nodeType}, State: ${n.state} ...")
      .mkString("\n")
    // ...
    s"""=== PLAGUENET SCENARIO EXPORT ===
      |Name: ${scenario.name}
      |Tick: ${scenario.tick}
      |...""".stripMargin
```
Grazie a questa netta separazione delle responsabilità, la persistenza diventa un modulo estensibile: aggiungere un 
nuovo formato di esportazione (ad esempio CSV o XML) richiede unicamente la creazione di un nuovo formato in `FileFormat`
e la fornitura delle relative implementazioni _given_ senza dover toccare minimamente né le logiche di dominio né la
struttura architetturale del framework.

### ExportPaths, FileFormat, PersistenceError

In aggiunta al motore di persistenza descritto sopra, mi sono occupato (interamente, a completamento del modulo di I/O)
della definizione strutturale dei formati supportati, del dominio degli errori e della gestione unificata dei percorsi
su file system.

#### FileFormat e PersistanceError
Per modellare i formati supportati e i tipi di errore, ho sfruttato il costrutto `enum` che fungono da Algebraic Data Types (ADT).
```scala
enum FileFormat:
  case Json, Txt

enum PersistenceError:
  case IO(message: String)
  case Parsing(message: String)
```

In `FileFormat`, i casi `Json` e `Txt` fungono da discriminanti tipizzati per i CodecCatalog, abilitando il pattern
matching esaustivo (il compilatore avvisa se si omette la gestione di un formato).

In `PersistenceError`, i casi `IO` e `Parsing` prendono dei parametri per veicolare il messaggio di errore specifico.
Questa enumerazione si sposa perfettamente con l'uso del costrutto `Either[PersistenceError, A]` usato in `Reader` e `Writer`,
permettendo al chiamante di destrutturare l'errore e capire immediatamente la sua natura.

#### ExportPaths
La generazione e la gestione dei percorsi per i salvataggi è stata centralizzata nel `singleton object ExportPaths`.
Questo componente previene la dispersione della logica di I/O nel resto dell'applicazione e garantisce coerenza.

Due aspetti implementativi sono particolarmente rilevanti in questo file:

- **Path Resolution Cross-Platform**: Utilizzando l'API `java.nio.file.Path`, la root directory viene risolta dinamicamente
tramite `System.getProperty("user.home")`, garantendo che l'applicazione scriva nella directory corretta `(splagnet)`
indipendentemente dal sistema operativo dell'utente (Windows, macOS o Linux).

- **Sanitizzazione dei File Name**: Poiché i nomi degli scenari sono scelti dall'utente, vi era il rischio di sollevare
eccezioni del file system o introdurre vulnerabilità di `Path Traversal` se l'utente avesse inserito caratteri
speciali _(es. Scenario/1: Test)_. La funzione `sanitizeFileName`:

```scala
private def sanitizeFileName(name: String): String =
  name.trim.replaceAll("""[\\/:*?"<>|]""", "_")
 ```
sfrutta le espressioni regolari (tramite tripli apici """ tipici di Scala per definire raw strings senza necessità di
escape multipli) per intercettare e sostituire con un underscore tutti i caratteri considerati illegali dai principali
file system, mettendo in sicurezza la creazione del file.

## Countermeasures ed Eventi

L'evoluzione dello scenario è guidata da una sequenza di **Eventi** che trasformano lo stato dello `Scenario`.
Ciascun evento applica regole di dominio ben definite (regole di difesa, distruzione, cura, rilevamento) e aggiorna 
la topologia in modo deterministico.


### Tratti, Mixin e Immutabilità del Dominio
Per evitare la duplicazione della logica di aggiornamento dei nodi all'interno della topologia, gli eventi sfruttano il pattern dei **Mixin** tramite il trait `TopologyUpdateMixin`.
Tutti gli eventi sono modellati come **oggetti immutabili** (istanze di `Event`) le cui funzioni restituiscono sempre un nuovo `Scenario` aggiornato:

```scala
object CountermeasureActivation:
  object ActivationEvent extends SimulationEvents.Event:
    override def apply(scenario: Scenario): Scenario =
      val config = scenario.countermeasureConfig
      val currentAwareness = scenario.awareness.value

      // Selezione funzionale delle contromisure da attivare
      val toBeActivated = config.countermeasureLevels
        .filter((threshold, counter) => currentAwareness >= threshold && !config.activeCountermeasures.contains(counter))
        .values
        .toSet

      scenario.copy(countermeasureConfig = config.copy(
        activeCountermeasures = config.activeCountermeasures ++ toBeActivated
      ))
```
Grazie ai metodi di `copy` e alle strutture dati immutabili di Scala, l'aggiornamento dello scenario (es. l'incremento 
dell'awareness in `Detection` o la cura in `Cure`) avviene senza alcun effetto collaterale (side effect), garantendo
la tracciabilità e la riesecuzione deterministica della simulazione.

### Separazione tra Regole di Dominio ed Esecuzione dell'Evento
La logica di calcolo matematico e stocastico (es. probabilità di distruzione, incremento del workload o probabilità di 
guarigione) è completamente disaccoppiata dagli eventi di simulazione ed è incapsulata in oggetti puri
dedicati (`DefenseRules`, `DestructionRules`).

Questa netta separazione garantisce che:

- **Le regole siano puramente funzionali**: Possono essere testate con unit test veloci e isolati senza dover istanziare
un intero oggetto Scenario.
- **Gli eventi siano meri orchestratori**: L'evento si occupa unicamente di verificare le precondizioni
(es. se la contromisura è attiva), raccogliere i nodi bersaglio e applicare le trasformazioni alla topologia.
- **Deterministico e Puro (Separazione del Seed)**: Gli eventi stocastici (come `CureEvent` o `DestroyEvent`) non
attingono a uno stato globale di numeri casuali (Random di sistema), ma istanziano un generatore pseudo-casuale
derivato in modo deterministico combinando il seed dello scenario, il tick corrente e l'identificativo univoco dell'evento
`(scenario.seed + scenario.tick + "cure".hashCode)`.

#### Esempio: Regola Pura (`DefenseRules`) vs Esecuzione dell'Evento (`CureEvent` e `PatchBoostEvent`)
Da un lato, l'oggetto DefenseRules isola la logica matematica pura senza gestire la topologia o il ciclo di vita dello scenario:

```scala
object DefenseRules:

// Funzione pura: incrementa il livello di patch rispettando il limite 1.0
def boostPatch(node: Node, config: CountermeasureConfig): Node =
  node.copy(patchLevel = math.min(1.0, node.patchLevel + config.patchBoostAmount))

// Calcolo puro della probabilità di cura
def cureProbability(node: Node, config: CountermeasureConfig): Probability =
  if Countermeasures.Patch.isApplicableTo(node.nodeType) then
    Probability.clamped(
      config.patchCureProbability + (1.0 - config.patchCureProbability) * node.patchLevel
    )
  else Probability.clamped(0.0)

// Risoluzione stocastica deterministica ricevendo il roll dall'esterno
def resolveCure(p: Probability, roll: Double): Boolean = roll < p.value
```
Dall'altro lato, gli eventi della simulazione delegano interamente i calcoli a DefenseRules e gestiscono esclusivamente la transizione dello stato di rete:

```scala
// 1. Evento deterministico diretto (Applicazione di Patch)
object PatchBoostEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
    val countermeasureConfig = scenario.countermeasureConfig
    
        if countermeasureConfig.activeCountermeasures.contains(Patch) then
          val updatedTopology =
            scenario.topology.healthyNodes().foldLeft(scenario.topology) { (currentTopology, node) =>
              updateNode(currentTopology, node.nodeId) { currentNode =>
                DefenseRules.boostPatch(currentNode, countermeasureConfig) // Invocazione regola pura
              }
            }
          scenario.copy(topology = updatedTopology)
        else scenario

// 2. Evento stocastico con isolamento del Seed (Cura dei Nodi)
object CureEvent extends Event with TopologyUpdateMixin:
    override def apply(scenario: Scenario): Scenario =
    val countermeasureConfig = scenario.countermeasureConfig
    
        if !countermeasureConfig.activeCountermeasures.contains(Patch) then return scenario
    
        // Derivazione deterministica del seme casuale locale per garantire la riproducibilità
        val rng = new Random(scenario.seed + scenario.tick + "cure".hashCode)
        val targets = scenario.topology.infectedNodes() ++ scenario.topology.quarantinedNodes()
    
        val updatedTopology = targets.foldLeft(scenario.topology) { (currentTopology, node) =>
          val curato = DefenseRules.resolveCure(
            DefenseRules.cureProbability(node, countermeasureConfig),
            rng.nextDouble()
          )
    
          if curato then
            updateNode(currentTopology, node.nodeId) { currentNode =>
              currentNode.copy(state = NodeState.Immune)
            }
          else currentTopology
        }
    
        scenario.copy(topology = updatedTopology)
```

### IsolationCriteria: Predicati Componibili e High-Order Functions
L'isolamento dei nodi infetti non segue una regola rigida, ma dipende da criteri configurabili dall'utente.
`IsolationCriteria` è modellato come una wrapper class attorno a una **Higher-Order Function** (un predicato `Node => Boolean`):

```scala
case class IsolationCriteria(matches: Node => Boolean):
    def and(other: IsolationCriteria): IsolationCriteria =
        IsolationCriteria(n => matches(n) && other.matches(n))

    def or(other: IsolationCriteria): IsolationCriteria =
        IsolationCriteria(n => matches(n) || other.matches(n))
```
Questo design sfrutta l'approccio delle Monoid/Combinator Functions:

- **Composibilità Fluida**: Tramite i metodi `and` ed `or`, l'utente può combinare liberamente più condizioni (es. isolare solo 
i nodi con workload elevato E appartenenti a un determinato NodeType).
- **Costruttori di Dominio**: Nel companion object vengono forniti generatori di criteri predefiniti
(`byType`, `byMinWorkload`, `byMaxDefense`) che restituiscono nuove funzioni predicato pronte per la composizione.

Durante l'esecuzione di `IsolationEvent`, il criterio viene applicato al set dei nodi infetti per identificare i bersagli,
aggiornare il loro stato in `Quarantined` e rimuovere selettivamente gli archi di connessione con il resto della rete 
(`cutEdgesOf`).

### FirewallPolicy
La contromisura del `Firewall` agisce a livello di connessioni (`Edge`), bloccando la propagazione sul canale di
comunicazione in base al tipo di canale (`ChannelType`) e al protocollo applicativo (`ApplicationProtocolType`).

La valutazione del blocco sfrutta il Pattern Matching di Scala 3 per gestire le eccezioni di dominio
(ad esempio, l'impossibilità di ispezionare il traffico all'interno di una VPN):

```scala
object FirewallPolicy:
  def isBlocked(edge: Edge, policy: FirewallPolicy): Boolean =
    if policy.blockedChannels.contains(edge.channel.channelType) then true
    else
      edge.channel.channelType match
        case Connection.ChannelType.VPN => false // Il traffico incapsulato in VPN non è ispezionabile
        case _ => edge.protocol.exists(p => policy.blockedApplicationProtocols.contains(p.kind))
```
## DSL interna per CountermeasureConfig
Similmente alla topologia, la dichiarazione delle contromisure beneficia di una DSL dichiarativa basata sulle context
functions (`?=>`), isolando l'utente dalla complessità della gestione del builder:

```Scala
def countermeasureConfig(
  block: CountermeasureConfigBuilder ?=> Unit
  ): ValidationResult[CountermeasureConfig] =
    given builder: CountermeasureConfigBuilder = new CountermeasureConfigBuilder()
    block(using builder)
    builder.build()
```
Grazie a questo approccio, i vari metodi della DSL intercettano il builder implicitamente. È stata inoltre sfruttata la
sintassi infix degli _extension methods_ per rendere la grammatica estremamente fluida e leggibile. Ad esempio:

```scala
extension (threshold: Double)
  infix def triggers(countermeasure: Countermeasures)(using
    builder: CountermeasureConfigBuilder
  ): Unit =
    builder.addLevel(threshold, countermeasure)
```
Che abilita la dichiarazione: `0.5 triggers Countermeasures.Isolation`.
Questo paradigma riduce il boilerplate code e accumula ogni configurazione nel builder mutabile,
che al termine produce un oggetto `CountermeasureConfig` immutabile e validato.