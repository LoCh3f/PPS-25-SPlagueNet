---
title: Specifica dei requisiti
nav_order: 3
parent: Report
---
# Specifica dei requisiti

Il presente capitolo definisce i requisiti funzionali, non funzionali, di business e il modello di dominio per il sistema **SplagueNet**. Il sistema si configura come un'applicazione desktop per la simulazione discreta della propagazione di minacce informatiche su topologie di rete configurabili, integrando meccanismi di difesa reattivi basati su soglie di awareness.

Il progetto si considera conforme e soddisfacente rispetto ai seguenti criteri di validazione:
1. **Riproducibilità deterministica**: a parità di scenario e seed, l'evoluzione della simulazione è perfettamente replicabile.
2. **Osservabilità delle relazioni causali**: la variazione isolata dei parametri (es. stealth del malware o soglie difensive) produce effetti coerenti e tracciabili sul sistema.
3. **Robustezza degli input**: gli scenari non validi vengono intercettati e rifiutati con segnalazioni esplicite, impedendo il raggiungimento di stati inconsistenti.
4. **Interattività e controllo**: l'utente dispone di un controllo completo sul ciclo di vita dell'esperimento (avvio, pausa, reset, confronto).
5. **Estendibilità del dominio**: l'architettura logica consente l'introduzione di nuove tipologie di nodi, vettori o contromisure senza alterare il motore di simulazione di base.
6. **Verificabilità**: la logica di dominio non grafica è coperta da test automatizzati integrati nella pipeline di CI.

## Requisiti di business

- Permettere di studiare l'interazione tra strategie offensive (tratti del malware) e difensive (contromisure di rete).
- Permettere di valutare l'esito di un'epidemia (compromissione totale vs. contenimento) in funzione di topologia, malware e policy di difesa.
- Rendere gli esperimenti ripetibili e confrontabili tra loro.
- Rendere il modello configurabile da utenti non programmatori (interfaccia grafica) e da programmatori (DSL).
- Fornire una restituzione sintetica e conservabile dell'esito di ogni esperimento (report).

## Modello di dominio

### Elementi statici

**Topology.** Grafo non orientato formato da nodi e connessioni. Gli identificativi dei nodi sono univoci; una connessione collega due nodi distinti esistenti, e tra due nodi esiste al più una connessione.

**Node.** Dispositivo della rete, caratterizzato da:
- *id*: stringa non vuota, senza spazi (gli spazi ai bordi vengono rimossi);
- *tipo*: Workstation, Server, Router, IoTDevice, MobileDevice;
- *patchLevel* e *defenseLevel*: livelli di protezione del nodo;
- *workload*: carico del nodo;
- *vectors*: vettori di propagazione a cui il nodo è esposto;
- *stato*: Healthy, Infected, Quarantined, Immune, Destroyed.

Ogni tipo di nodo ha due coefficienti: il coefficiente di **rilevazione** (capacità di accorgersi di anomalie) e la **vulnerabilità strutturale**.

| Tipo | Rilevazione | Vulnerabilità strutturale |
|------|------------:|--------------------------:|
| Workstation | 1.0 | 1.0 |
| Server | 1.5 | 0.8 |
| Router | 0.8 | 1.0 |
| IoTDevice | 0.3 | 1.3 |
| MobileDevice | 0.9 | 1.0 |

**Connection.** Ogni connessione ha un *canale* (LAN, WAN o VPN) con banda, latenza, jitter e probabilità di perdita di pacchetto, e può opzionalmente dichiarare un *protocollo applicativo* (HTTP, HTTPS, FTP, SSH, IMAP, Telnet), a sua volta basato su un protocollo di trasporto (TCP, affidabilità 0.99; UDP, affidabilità 0.90). Se i parametri non sono specificati, valgono i valori di default del tipo di canale:

| Canale | Banda | Latenza | Jitter | Perdita |
|--------|------:|--------:|-------:|--------:|
| LAN | 1000 | 1 | 0.1 | 0.001 |
| WAN | 100 | 50 | 5 | 0.01 |
| VPN | 200 | 30 | 3 | 0.005 |

**Malware.** Entità con nome, *tipo* (Worm o Virus), *vettori di propagazione* (NetworkExploit, Phishing, Usb, SupplyChain; almeno uno) e tratti:
- *infectivity*, *stealth*, *persistence*, *footprint*: probabilità in [0,1];
- *payload severity*: Low, Medium o High.

**Countermeasures.** Quattro contromisure: **Patch** (aumenta il patch level dei nodi sani e cura i nodi infetti o in quarantena), **DefenseBoost** (aumenta il defense level dei nodi sani), **Isolation** (mette in quarantena i nodi infetti) e **Firewall** (rimuove le connessioni bloccate). Patch e DefenseBoost non sono applicabili ai nodi IoTDevice.
La loro configurazione comprende:
- l'insieme delle contromisure attive fin dall'inizio;
- le *soglie di awareness* oltre le quali ciascuna contromisura si attiva;
- l'incremento di patch (default 0.05) e di defense (default 0.05) per tick;
- la probabilità base di cura (default 0.5);
- il *criterio di isolamento*, componibile con and/or: tutti i nodi, per tipo, per workload minimo, per defense massimo;
- la *policy del firewall*: canali e protocolli applicativi bloccati (default: WAN, Telnet, FTP). Il traffico su VPN non è ispezionabile: viene bloccato solo se il canale VPN è esplicitamente bloccato.

**Awareness.** Valore in [0,1] che misura la consapevolezza della rete della presenza del malware.

**Scenario.** Aggregato che contiene nome (non vuoto), topologia, malware, nodo di partenza (appartenente alla topologia), tick corrente, seed, numero massimo di iterazioni (> 0, default 20), awareness, configurazione delle contromisure e workload di riferimento (*baseline*) di ogni nodo, calcolato alla creazione.

**Report.** Sintesi di una simulazione: nome dello scenario, seed, malware, serie temporale per tick (numero di nodi per stato e awareness), tick di attivazione di ogni contromisura e *milestone* (primo tick di diffusione, picco di infezioni, primo tick con nodi distrutti).

### Elementi dinamici

La simulazione è a tempo discreto. Il nodo di partenza viene marcato come infetto (*paziente zero*) all'avvio e la simulazione produce gli stati dal tick 0 al tick `maxIterations`. A ogni tick viene eseguito **un gruppo di eventi**, scelto ciclicamente in base a `tick mod 4`:

| Fase | Eventi |
|------|--------|
| 0 | **Detection**, **attivazione delle contromisure** |
| 1 | **Isolation**, **Firewall** |
| 2 | **Infezione**, incremento di patch, incremento di defense |
| 3 | **Cura**, riduzione del workload dei nodi immuni, aumento del workload dei nodi infetti, **distruzione** |

Le regole di ciascun evento sono le seguenti.

- **Detection:** Il *segnale di rilevazione* è la somma, sui nodi infetti, di `workload × coefficiente di rilevazione × (1 − stealth)`, divisa per il numero totale di nodi. L'awareness si avvicina al segnale con rate 0.3: `awareness' = awareness + 0.3 × (segnale − awareness)`; non supera mai il segnale e resta in [0,1].
- **Attivazione:** Ogni contromisura con soglia minore o uguale all'awareness corrente diventa attiva e non si disattiva più.
- **Infezione:** Ogni nodo infetto tenta di infettare i vicini sani che condividono almeno un vettore con il malware. La probabilità è `infectivity × (1 − defense) × (1 − patch) × vulnerabilità strutturale × (1 − perdita del canale) × affidabilità del trasporto` (l'ultimo fattore solo se la connessione dichiara un protocollo), limitata a [0,1].
- **Isolation** (se attiva): I nodi infetti che soddisfano il criterio passano in Quarantined e le loro connessioni vengono rimosse.
- **Firewall** (se attivo): Le connessioni bloccate dalla policy, unita alla policy di default, vengono rimosse.
- **Patch / DefenseBoost** (se attive): Incrementano i livelli dei nodi sani fino a un massimo di 1.0.
- **Cura** (se Patch attiva): Ogni nodo infetto o in quarantena diventa Immune con probabilità `p + (1 − p) × patchLevel`, dove `p` è la probabilità base di cura; è 0 per i nodi IoTDevice.
- **Workload:** Il workload dei nodi infetti cresce di `footprint × fattore di severità` (0.1, 0.2, 0.4 per Low, Medium, High), fino a 1.0. Quello dei nodi immuni cala di 0.05 per tick, senza scendere sotto la baseline.
- **Distruzione:** Ogni nodo infetto viene distrutto con probabilità `workload / vulnerabilità strutturale` (limitata a [0,1]).

Tutte le scelte casuali derivano dal seed dello scenario e dal tick corrente, quindi sono riproducibili.

**Ciclo di vita della simulazione:** Una simulazione può essere *non avviata*, *in esecuzione*, *in pausa* o *terminata* (tick massimo raggiunto). Dalla pausa si può riprendere o azzerare; dallo stato terminato si può solo azzerare. L'azzeramento riporta lo scenario allo stato iniziale, con tutti i nodi sani.

## Requisiti funzionali

### Requisiti utente

- L'utente può scegliere uno scenario tra quelli disponibili. Sono precaricati "Linear chain" (4 workstation in catena) e "Complex enterprise mesh" (12 nodi eterogenei).
- L'utente può aggiungere, modificare e rimuovere nodi (id, tipo, patch, defense, stato, workload, vettori).
- L'utente può aggiungere, modificare e rimuovere connessioni (canale e parametri di rete) tra due nodi distinti, senza duplicati.
- L'utente può aggiungere in un colpo solo una topologia notevole a 5 nodi (star, ring o mesh).
- L'utente può configurare nome, seed, numero massimo di iterazioni e nodo di partenza dello scenario.
- L'utente può configurare il malware (nome, tipo, tratti, severità, vettori).
- L'utente può configurare le contromisure: attivazione iniziale, soglie di awareness, criterio di isolamento, policy del firewall.
- L'utente può salvare lo scenario modificato nell'elenco degli scenari; la modifica o il salvataggio con lo stesso nome sostituisce la voce esistente, mentre una rinomina non lascia duplicati.
- L'utente può avviare la simulazione, metterla in pausa, riprenderla e azzerarla.
- L'utente vede in tempo reale lo stato di ogni nodo (colore), il tick corrente e l'awareness in percentuale.
- L'utente può navigare il workspace: zoom, pan, selezione, trascinamento dei nodi.
- L'utente può consultare il report di una simulazione, salvarlo su file e importarne uno salvato.
- L'utente può esportare lo scenario (JSON o TXT) e importarlo (JSON).
- Qualunque input non valido viene segnalato con un messaggio che indica il campo e il motivo.

*Comandi del workspace:* rotellina per lo zoom; click e trascinamento su spazio vuoto per il pan; click sinistro per selezionare nodo o connessione; trascinamento per spostare un nodo; Ctrl + click su spazio vuoto per creare un nodo; click destro per modificare; Delete per rimuovere; Shift + trascinamento da un nodo a un altro per creare una connessione.

### Requisiti di sistema

- Il sistema valida identificativi, probabilità, soglie e vincoli di scenario (vedi modello di dominio) e rifiuta i valori non validi; i valori inseriti dall'utente non vengono corretti in silenzio.
- Le DSL accumulano tutti gli errori di una dichiarazione (nodi, archi, malware, scenario) invece di fermarsi al primo.
- Il sistema esegue gli eventi di simulazione secondo la tabella delle fasi e le regole descritte nel modello di dominio.
- Il sistema produce lo stesso risultato a parità di scenario e seed.
- Il sistema avanza la simulazione a intervalli regolari (2,5 s per tick) e ignora gli avanzamenti mentre la simulazione è in pausa o terminata.
- Il sistema impedisce la modifica della topologia e il cambio di scenario mentre la simulazione è in esecuzione, e blocca il pannello di configurazione finché la simulazione non viene azzerata.
- Il sistema rifiuta l'avvio di una nuova simulazione se ne esiste già una, finché non viene azzerata.
- Il sistema rende disponibile il report solo se la simulazione non è in esecuzione.
- Il sistema calcola il report rieseguendo in modo deterministico lo scenario iniziale.
- Il sistema salva i file di esportazione e dei report nella cartella `splagnet` della home dell'utente, con nomi derivati da quello dello scenario (caratteri non ammessi sostituiti da `_`).
- Il sistema segnala gli errori di lettura, scrittura e parsing dei file senza interrompersi.

## Requisiti non funzionali

- **Riproducibilità:** Due esecuzioni con lo stesso scenario e lo stesso seed producono la stessa sequenza di stati. *(Verifica: Test automatici sul report e sul motore)*
- **Correttezza dei dati:** Nessuna probabilità, awareness o livello di protezione può uscire da [0,1]. *(Verifica: Test unitari sulle regole di dominio)*
- **Usabilità:** Costruire una topologia e avviare una simulazione richiede solo l'interfaccia grafica, con la guida dei comandi in `USERGUIDE.md`. *(Verifica: Verifica manuale con l'esperto di dominio)*
- **Reattività:** L'interfaccia resta utilizzabile durante la simulazione. *(Verifica: Verifica manuale su "Complex enterprise mesh")*
- **Portabilità:** Il sistema funziona su ogni sistema operativo con JDK 21 o superiore. *(Verifica: CI su `ubuntu-latest`; esecuzione manuale su altri sistemi)*
- **Qualità del codice:** `./gradlew check` termina senza errori (compilazione, test, analisi statica SpotBugs, formattazione scalafmt). *(Verifica: CI a ogni push)*
- **Testabilità:** La logica di dominio e di aggiornamento è separata dalla vista, così da poterla verificare senza interfaccia grafica; i test coprono il codice non grafico. *(Verifica: Report Scoverage, escluso il package `view`)*
- **Estendibilità:** Aggiungere un nuovo evento di simulazione o un nuovo tipo di nodo non richiede modifiche al motore. *(Verifica: Revisione del design)*
- **Scalabilità (dimensione della rete):** Il sistema simula senza degrado percepibile reti fino a *N* nodi (da misurare e indicare qui). *(Verifica: Misura sul simulatore)*

*(Nota: Il sistema non è distribuito, si tratta di un'unica applicazione locale, quindi non si pongono requisiti di robustezza a guasti di rete o di scalabilità orizzontale.)*

## Requisiti di implementazione

- Il linguaggio è Scala 3, con approccio funzionale: dati immutabili, funzioni pure per la logica di dominio, errori modellati con `Either`.
- La build è gestita con Gradle; i test sono scritti con ScalaTest e sono eseguiti in integrazione continua.
- L'interfaccia grafica è realizzata con Scala Swing.
- La persistenza usa JSON (Circe) e testo semplice, con codec separati dal modello di dominio.
- Il progetto richiede JDK 21 o superiore.
- Il codice è sottoposto ad analisi statica (SpotBugs) e a formattazione automatica (scalafmt).