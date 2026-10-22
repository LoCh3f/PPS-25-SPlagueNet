---
title: Specifica dei requisiti
nav_order: 3
parent: Report
---

# Specifica dei requisiti

Il presente capitolo definisce i requisiti di business, il modello di dominio, le dinamiche di simulazione e l'insieme dei requisiti funzionali, non funzionali e di implementazione che guidano lo sviluppo e la validazione del sistema **SplagueNet**.

## Requisiti di business

**SplagueNet** nasce come un simulatore desktop pensato per esplorare in modo interattivo e rigoroso la dinamica epidemica all'interno di infrastrutture di rete configurabili. L'intento principale è offrire uno strumento d'analisi flessibile, capace di mettere a confronto le strategie offensive (rappresentate dai tratti evolutivi del malware) e quelle difensive (tradotte in contromisure reattive automatiche), valutando se un'infezione conduca alla compromissione totale del sistema o se venga efficacemente contenuta.

I requisiti di business fondamentali individuati per il progetto comprendono:
- **Simulazione e analisi**: il sistema deve permettere di valutare l'esito di un'epidemia in funzione della topologia di rete, dei tratti del malware e delle policy di difesa adottate.
- **Ripetibilità**: gli esperimenti devono poter essere replicati e confrontati con assoluta precisione; a parità di scenario e di seed, la simulazione deve produrre sempre la medesima sequenza di stati.
- **Sintesi dei risultati**: al termine di ogni esperimento, il sistema deve generare un report chiaro, sintetico e conservabile che riassuma l'andamento e le metriche chiave dell'epidemia.
- **Modularità ed estendibilità**: l'architettura deve essere progettata secondo criteri di netta separazione delle responsabilità, in modo da consentire l'aggiunta di nuove tipologie di nodi, vettori di propagazione o contromisure senza richiedere modifiche al motore di simulazione di base.

## Modello di dominio

- **Topology**: rappresenta la struttura della rete come un grafo non orientato composto da nodi e connessioni. Ciascun `Node` identifica un dispositivo specifico: workstation, server, router, dispositivo IoT o mobile. Le `Connection` collegano coppie di nodi distinti (al più una connessione per coppia) mediante canali differenziati (LAN, WAN o VPN) caratterizzati da parametri propri.
- **Malware**: l'entità minacciosa che guida l'infezione. È caratterizzata da un tipo (worm o virus), da almeno un vettore di propagazione e da un insieme di tratti distintivi che determinano la sua aggressività, la difficoltà di individuazione e l'impatto distruttivo sui dispositivi compromessi.
- **Countermeasures e Awareness**: la risposta difensiva della rete si attiva dinamicamente quando il livello di `Awareness` (una metrica normalizzata in $[0,1]$ che misura la consapevolezza globale del rischio) supera determinate soglie. Le contromisure disponibili sono quattro: *Patch*, *DefenseBoost*, *Isolation* e *Firewall*.
- **Scenario e Report**: lo scenario costituisce l'aggregatore globale che raccoglie la topologia, il malware configurato, il nodo di partenza, il seed casuale e i limiti di iterazione. Il `Report`, generato al termine della simulazione, aggrega le serie temporali per tick, i momenti di attivazione delle contromisure e le milestone critiche dell'epidemia, come il primo punto di diffusione o il picco massimo delle infezioni.

## Requisiti funzionali

### Requisiti utente
- L'utente deve poter scegliere uno scenario all'interno di un insieme precaricato (quali "Linear chain" e "Complex enterprise mesh") o procedere alla creazione e al salvataggio di nuove configurazioni.
- L'utente deve poter costruire e modificare interattivamente la topologia di rete, aggiungendo, spostando o rimuovendo nodi e connessioni, con la facoltà di generare rapidamente topologie strutturate predefinite (star, ring o mesh).
- L'utente deve poter configurare parametricamente lo scenario, le proprietà del malware e le regole delle contromisure.
- L'utente deve disporre di comandi completi per gestire il ciclo di vita della simulazione, potendola avviare, mettere in pausa, riprendere o azzerare.
- L'utente deve poter monitorare in tempo reale l'evoluzione visiva dello stato dei nodi, il contatore dei tick e il livello di awareness, navigando agevolmente nel workspace grafico.
- L'utente deve poter consultare il report riepilogativo di una simulazione completata, salvarlo su supporto persistente o importare report precedentemente archiviati.
- L'utente deve poter esportare gli scenari in formati standard (JSON o TXT) e reimportarli agevolmente nel sistema.

### Requisiti di sistema
- Il sistema deve validare rigorosamente identificativi, probabilità, soglie e vincoli strutturali dello scenario, rifiutando i valori scorretti senza applicare correzioni silenziose.
- Il sistema deve eseguire la logica di simulazione attenendosi strettamente alla sequenza delle fasi previste, avanzando a intervalli regolari e sospendendo l'elaborazione qualora la simulazione si trovi in pausa o abbia raggiunto il termine.
- Il sistema deve garantire la coerenza e l'identicità dei risultati ottenuti a parità di condizioni di input (scenario e seed).
- Il sistema deve rendere accessibile la generazione e consultazione del report esclusivamente al di fuori dei momenti di esecuzione attiva della simulazione, calcolandolo tramite una riesecuzione deterministica basata sullo scenario iniziale.
- Il sistema deve gestire la persistenza salvando esportazioni e report all'interno di una cartella dedicata (`splagnet`) nella home directory dell'utente, gestendo eventuali anomalie di I/O o di parsing in modo sicuro e senza causare interruzioni anomale dell'applicazione.

## Requisiti non funzionali

I requisiti non funzionali del sistema sono associati a specifici criteri di verifica e misurazione:

- **Riproducibilità**: due esecuzioni consecutive condotte con lo stesso scenario e lo stesso seed devono produrre una sequenza identica di stati. *Misurazione*: esecuzione di test automatici mirati sul motore di simulazione e sui report.
- **Correttezza dei dati**: nessun valore di probabilità, awareness o livello di protezione può fuoriuscire dall'intervallo normalizzato $[0,1]$. *Misurazione*: test unitari a copertura delle regole di dominio.
- **Usabilità**: la costruzione di una topologia e l'avvio di una simulazione devono poter essere compiuti interamente attraverso l'interfaccia grafica. *Misurazione*: sessioni di prova manuale condotte con il supporto dell'esperto di dominio.
- **Reattività**: l'interfaccia utente deve preservare la propria fluidità e usabilità anche durante l'elaborazione di reti complesse. *Misurazione*: verifica manuale basata sullo scenario stress "Complex enterprise mesh".
- **Qualità del codice**: l'esecuzione del comando di build `./gradlew check` deve concludersi senza segnalazioni di errore, superando compilazione, test unitari, analisi statica con SpotBugs e formattazione con scalafmt. *Misurazione*: esecuzione automatica nella pipeline di Continuous Integration a ogni push.
- **Testabilità**: la logica di dominio e di aggiornamento deve essere rigorosamente disaccoppiata dalla vista grafica e verificabile in modo indipendente. *Misurazione*: generazione di report di code coverage tramite Scoverage (escludendo unicamente il package della vista).
- **Estendibilità**: l'introduzione di un nuovo evento di simulazione o di una nuova tipologia di nodo non deve imporre modifiche strutturali al motore core. *Misurazione*: revisione periodica del design architetturale.

## Requisiti di implementazione

SplagueNet è scritto in **Scala 3**, con uno stile funzionale: i dati non vengono modificati ma sostituiti con nuove copie, la logica di dominio è fatta di funzioni senza effetti collaterali e gli errori sono restituiti esplicitamente con il tipo `Either`, senza lanciare eccezioni.

La build e le dipendenze sono gestite con **Gradle**. I test sono scritti con **ScalaTest** e vengono eseguiti automaticamente dalla CI a ogni push. L'interfaccia grafica usa **Scala Swing**.

Per salvare e caricare i dati si usa **Circe** per il formato JSON, oltre a un formato testuale semplice. I codec sono tenuti separati dal modello di dominio, che quindi non dipende dal formato dei file.