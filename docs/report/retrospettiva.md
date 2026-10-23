---
title: Retrospettiva
nav_order: 8
parent: Report
---
# Retrospettiva

## Sprint 1: Avvio e Regole Base

### Descrizione dell’andamento dello sviluppo
Questo primo sprint è servito ad avviare il progetto. Abbiamo creato la repository, preparato i sistemi per compilarlo 
e configurato i test automatici. Inoltre, abbiamo definito le entità base della simulazione: come si comportano i nodi,
i tipi di connessioni e le caratteristiche del virus. Il lavoro si è concluso con un ambiente pronto per la programmazione.

### Backlog
Il backlog conteneva le attività per iniziare:
* Set Up Repo and Git Hooks (completato in 2 ore da AV).
* Set Up build system (completato in 4 ore da AV).
* Definition of connections, channel and protocols (completato in 6 ore da AV).
* Definition of malware traits and behavior (completato in 8 ore da GP).
* Docs (completato in 5 ore da GP).
* CI/CD (completato in 8 ore da EA).
* Definition of nodes and behavior (completato in 7 ore da EA).

### Iterazioni
Abbiamo lavorato rapidamente per risolvere i blocchi iniziali, soprattutto su repository e test automatici. Dividere i 
compiti tecnici da quelli teorici ha permesso a tutti di lavorare contemporaneamente senza ostacolarsi.

### Commenti sprint
Sprint utile per preparare il terreno. Le stime di tempo per le configurazioni erano corrette, permettendoci di passare
allo sviluppo vero e proprio nello sprint successivo.

## Sprint 2: Motore di Simulazione e Struttura Dati

### Descrizione dell’andamento dello sviluppo
In questo sprint abbiamo costruito il motore principale del simulatore e il sistema per aggiornare i dati. Abbiamo
aggiunto le regole per le infezioni e definito la classe Probability. Infine, abbiamo inserito funzioni per controllare la "salute" dei nodi.

### Backlog
Le attività riguardavano la logica centrale:
* Controllo e gestione della simulazione, Engine e State (completato in 12 ore da AV).
* Define the Update function(MVU) and Scenario (completato in 10 ore da EA).
* Simulation Logic (func for infection, probability) (completato in 14 ore da GP).
* Introduce the awareness (works with healthy checker) (completato in 8 ore da GP).
* Healthy Checker Event (completato in 6 ore da GP).

### Iterazioni
Abbiamo affrontato compiti difficili. La funzione di aggiornamento ha richiesto molte verifiche per funzionare bene con
il motore principale. I test fatti in corso d'opera hanno confermato che le regole di infezione erano corrette.

### Commenti sprint
Uno sprint impegnativo. Il design del simulation engine e del MVU ha richiesto più tempo del previsto. E' stato necessario
fare una standup più lunga del solito per chiarire bene le idee.

## Sprint 3: Eventi, Impostazioni e Prima Schermata

### Descrizione dell’andamento dello sviluppo
Abbiamo creato i file per le impostazioni generali, gestito le contromisure e realizzato una prima schermata 
visibile del simulatore. Abbiamo anche migliorato i controlli, aggiungendo un firewall, test per verificare che tutto 
funzioni insieme, detection per i nodi infetti e la test coverage.

### Backlog
Il backlog includeva grafiche di base, regole di difesa e controlli:
* Basic View for simulation (completato in 10 ore da AV).
* Create a Config file with constants (completato in 4 ore da GP).
* Define Countermeasure Event with rules (completato in 6 ore da EA).
* Firewall + integration test (completato in 4 ore da EA).
* Infected nodes for detection signal (completato in 3 ore da EA).
* Add code coverage (completato in 1 ore da EA).

### Iterazioni
Abbiamo alternato la scrittura di nuove regole con la creazione di test per 
verificarle. Lavorando in questo modo, siamo riusciti a collegare la schermata visiva senza bloccare lo sviluppo delle regole
della simulazione.

### Commenti sprint
Un buon equilibrio tra grafica e logica. Avere più test ci ha dato maggiore sicurezza nel modificare il codice e aggiungere regole.

## Sprint 4: Topologie di default, DSL e Salvataggio

### Descrizione dell’andamento dello sviluppo
Questo sprint è servito a rendere il simulatore più pratico da usare. Abbiamo creato una DSL per definire topologie e scenari in modo più semplice.
Abbiamo aggiunto la possibilità di salvare e caricare topologie, scenari su file e migliorato il modo in cui il virus si diffonde.

### Backlog
Le attività coprivano DSL, salvataggi e default topologies:
* Default topologies con DSL (completato in 8 ore da AV).
* Default scenarios (completato in 6 ore da AV).
* DSL (completato in 8 ore da GP).
* Add topology shapes (DSL) (completato in 8 ore da GP).
* Refactor propagation (completato in 1 ora da EA).
* save/load scenario da/a file (completato in 10 ore da EA).
* Modifica test-coverage (completato in 4 ore da EA).

### Iterazioni
Costruire la DSL ha richiesto diverse prove per renderli semplici da scrivere. I problemi tecnici ci 
hanno rallentato un po', ma abbiamo recuperato organizzando meglio il tempo.

### Commenti sprint
Nonostante gli intoppi tecnici, abbiamo completato funzioni importanti. Il salvataggio e le topologie predefinite permettono
finalmente di provare il simulatore comodamente.

## Sprint 5: Nuova Grafica e Moduli

### Descrizione dell’andamento dello sviluppo
Abbiamo cambiato la grafica, passando a una nuova libreria. Insieme a questo, abbiamo creato i menu a schermo per inserire
le difese e controllare le probabilità dei virus. Abbiamo anche ampliato i comandi semplificati per gestire scenari interi
e permesso di salvare le partite mentre sono in corso.

### Backlog
Il backlog si concentrava su menu, grafica e comandi:
* save/load scenario a runtime (completato in 6 ore da EA).
* report per scenario (completato in 5 ore da GP).
* Validate the malware probability (form) (completato in 3 ore da GP).
* DSL for scenario (completato in 2 ore da GP).
* Scala Swing migration (completato in 15 ore da AV).
* Countermeasure form (UI) (completheir types tato in 3 ore da EA).
* Countermeasure DSL (completato in 3 ore da EA).
* modifica regola awareness (completato in 1 ore da EA).

### Iterazioni
Il cambio di grafica è stato il lavoro principale. Le altre attività sono state modificate per funzionare con i nuovi menu.
Abbiamo provato la schermata più volte per renderla comoda da usare.

### Commenti sprint
Uno sprint faticoso per via del cambio di grafica. Tuttavia, il risultato ha migliorato molto l'aspetto del programma e
la facilità di utilizzo.

## Sprint 6: Correzioni e Conclusione

### Descrizione dell’andamento dello sviluppo
L'ultimo sprint è stato usato per pulire il codice, sistemare l'interfaccia visiva e scrivere i documenti finali. 
Abbiamo riordinato la struttura dei dati, uniformato le view a schermo e risolto piccoli errori sui menu.
Abbiamo anche aggiunto il tasto per fermare la simulazione e sistemato i livelli di difesa per i nuovi nodi inseriti.

### Backlog
Le attività coprivano pulizia del codice, risoluzione di errori e documenti:
* refactor mvu (completato in 6 ore da AV).
* First infected node (completato in 2 ore da AV).
* Extract shared UI helpers and unify editor dialogs (completato in 6 ore da AV).
* Stop Button (completato in 1 ore da AV).
* Organize all the config values (completato in 5 ore da GP).
* Fix CountermeasureForm (completato in 4 ore da EA).
* New nodes get defenseLevel = 1.0 (completato in 1 ore da EA).
* activeCMs are parsed but not used (completato in 2 ore da EA).
* Doc processo di sviluppo (completato in 4 ore da EA).

### Iterazioni
Ci siamo concentrati sul trovare e correggere velocemente gli ultimi errori. Le modifiche al codice sono state fatte con
molta attenzione per non rompere nulla a fine progetto.

### Commenti sprint
La cura per i dettagli ci ha permesso di consegnare un prodotto completo. Mettere in ordine il codice e le impostazioni 
rende il programma più facile da aggiornare in futuro.

## Commenti finali
Il gruppo ha lavorato con costanza, rispettando le circa 15 ore settimanali previste per ogni persona in tutti i 6 sprint.
Gestendo bene i compiti, siamo passati da un'impostazione vuota a un simulatore completo, dotato di salvataggio dati e di
una buona grafica interattiva. Abbiamo trovato il giusto equilibrio tra l'aggiunta di nuove funzioni e il mantenimento
di un codice ordinato, ottenendo un risultato finale solido e facile da usare.

## Sviluppi Futuri

### Migliorare UI/UX

L'interfaccia attuale copre l'intero ciclo di utilizzo, ma ci sono margini di miglioramento
sia nella chiarezza sia nella comodità d'uso. Un primo intervento riguarda la leggibilità
del workspace: una legenda dei colori associati agli stati dei nodi e un'indicazione visiva
più esplicita delle connessioni bloccate o tagliate dalle contromisure aiuterebbero a
seguire l'evoluzione dell'epidemia.

### Comparare più Report

Il report è già salvabile e importabile, ma può essere consultato solo uno alla volta.
Poiché a parità di scenario e di seed la simulazione è riproducibile, un'evoluzione naturale
è il confronto affiancato di più report: ad esempio lo stesso scenario con policy di
contromisure diverse, oppure lo stesso malware su topologie differenti. Il confronto
potrebbe sovrapporre le timeline dei nodi infetti e mostrare le differenze nei tick di
attivazione delle contromisure e nelle milestone, rendendo più immediata la valutazione
di quale strategia difensiva sia più efficace.

### Applicare la differenza tra Virus/Malware

Nel modello la distinzione tra `Worm` e `Virus` è già presente (il tipo `MalwareKind`
espone `requiresTrigger`), ma oggi non viene sfruttata dal motore di simulazione: i due
tipi si propagano allo stesso modo. Un'estensione possibile è far rispettare la differenza
nelle regole di contagio: il worm continuerebbe a diffondersi autonomamente attraverso la
rete, mentre il virus avrebbe bisogno di un'attivazione (ad esempio l'interazione di un
utente o un supporto esterno) prima di infettare i nodi vicini.
