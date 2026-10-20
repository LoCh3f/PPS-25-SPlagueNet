---
title: Processo di Sviluppo
nav_order: 2
parent: Report
---
# Processo di sviluppo

## Metodologia
Come metodologia di sviluppo, è stata adottata la metodologia SCRUM, proposta dalle regole d’esame al punto P8.

### Ruoli

- **Product Owner** - Aldo Visconti
- **Esperto di dominio** - Eduard Toni Alexandru
- **Scrum Master** - Giovanni Paradisi

Tutti i membri ricoprono il ruolo di **Development Team** e contribuiscono in modo
equilibrato all'implementazione del progetto.

## Gestione e Pianificazione del Lavoro

Le attività di progetto sono strutturate in cicli iterativi **(Sprint)** della durata di una settimana, con un impegno stimato
di 15 ore per ciascun membro.

Ciascuno Sprint prevede tre fasi principali:

- Sprint Planning: Pianificazione e selezione dei task, stima dell'effort richiesto e assegnazione delle responsabilità.
- Sprint Review: Verifica dei risultati conseguiti rispetto agli obiettivi prefissati e riscontro con la Definition of Done.
- Sprint Retrospective: Analisi critica del flusso di lavoro, condivisione dei riscontri con gli stakeholder e 
individuazione di azioni migliorative per i cicli successivi.

## Definition of done
Un task si considera completato quando:

- il codice compila senza warning;
- i test relativi alla funzionalità sono presenti e passano in CI;
- la Pull Request è stata approvata.
## Strumenti

| Strumento | Utilizzo                           | 
| ----- |------------------------------------| 
| Git + GitHub | Versionamento del codice           | 
| Gradle | Build tool e gestione dipendenze   | 
| GitHub Actions | CI/CD                              | 
| ScalaTest | Testing unitario e di integrazione | 
| IntelliJ IDEA | IDE di sviluppo                    | 

## Versionamento e Strategia di Branching

### Conventional Commits
Per i commit si adotta la convenzione **Conventional Commits**:

- feat: nuova funzionalità
- fix: correzione di bug
- docs: aggiornamento documenti relativi al progetto
- chore: manutenzione varia, configurazioni, dipendenze, modifiche workflow

### Strategia di Branching (Git Flow)
Il flusso di lavoro si basa su un modello ispirato a **Git Flow**, che prevede l'utilizzo dei seguenti rami
principali e di supporto:

- **main:** Branch stabile e protetto, contenente esclusivamente il codice rilasciato in produzione o pronto
per le release ufficiali.
- **develop**: Branch di integrazione principale, in cui confluiscono tutte le modifiche completate e testate prima
di essere promosse verso il rilascio.
- **feature/**: Branch temporanei creati a partire da develop (secondo la convenzione _feature/nome-attivita_)
dedicati allo sviluppo di singole funzionalità o task specifici.

Una volta completata l'implementazione di una feature sul proprio branch dedicato, viene aperta una **Pull Request**
verso il branch develop. Analogamente, a conclusione di ogni sprint, viene predisposta un'ulteriore Pull Request di 
consolidamento su develop per verificare l'integrazione complessiva dei task completati prima della chiusura del ciclo.

## Continuous Integration (CI)
La pipeline di Continuous Integration viene attivata automaticamente a ogni push sui branch `main`, `develop` e sui branch 
di tipo `feature/**` (escludendo le modifiche a file di documentazione come `README.md`, `CHANGELOG.md` e la cartella `docs/**`),
a ogni apertura di una `pull_request` verso main o develop, oppure tramite attivazione manuale (`workflow_dispatch`).

Il processo esegue i seguenti passaggi:

- **Checkout del codice**: Sincronizzazione del repository con l'ambiente di esecuzione (su sistema operativo `ubuntu-latest`).
- **Configurazione dell'ambiente**: Installazione e configurazione di Java JDK (Eclipse Temurin, versione 21) con caching delle dipendenze Gradle per ottimizzare i tempi di esecuzione.
- **Preparazione dei permessi**: Configurazione dei permessi di esecuzione per lo script wrapper di Gradle (`gradlew`).
- **Controllo qualità e test**: Esecuzione delle verifiche di qualità del codice e dei test tramite il comando `./gradlew check`.
- **Generazione del report di copertura**: Esecuzione del build e generazione del report di code coverage con Scoverage (`./gradlew reportScoverage`).
- **Caricamento degli artefatti**: Upload dei report di Scoverage come artefatti di build.

## Continuous Delivery (CD)

La pipeline di **Continuous Delivery** si attiva automaticamente in seguito a un `push` sul branch `main` o tramite 
esecuzione manuale (`workflow_dispatch`).

Il flusso operativo comprende le seguenti fasi:

- **Checkout completo**: Download del repository con storico completo (fetch-depth: 0) per consentire la corretta
analisi dei commit ai fini del rilascio.
- **Setup dell'ambiente**: Configurazione di JDK 21 e caching delle dipendenze Gradle, con abilitazione dei permessi di
esecuzione per `./gradlew`.
- **Packaging dell'applicazione**: Compilazione ed emersione di un archivio eseguibile di tipo fat JAR tramite il task
Gradle `./gradlew app:shadowJar`.
- **Rilascio automatizzato**: Esecuzione di Semantic Release (tramite l'azione dedicata con i plugin per changelog,
git e GitHub), che provvede ad analizzare i messaggi di commit, aggiornare la versione, generare il changelog e 
pubblicare automaticamente la release su GitHub con i relativi artefatti.

## Documentazione e Pubblicazione (GitHub Pages)
È stata inoltre introdotta una pipeline dedicata alla gestione e al rilascio automatico della documentazione di 
progetto tramite Jekyll e GitHub Pages. Il flusso si attiva automaticamente a ogni `push` sul branch `main` che
interessi i file all'interno della cartella `docs/**` o il file di workflow dedicato, oppure manualmente.

Il processo si articola nei seguenti punti:

- **Preparazione dell'ambiente**: Checkout del repository e configurazione dell'ambiente Ruby (versione 3.3) con 
caching di Bundler per la gestione delle dipendenze di Jekyll.
- **Build del sito statico**: Generazione del sito di documentazione tramite il comando `bundle exec jekyll build`.
- **Configurazione del dominio**: Inserimento del file `.nojekyll` per inibire il processamento predefinito di Jekyll 
da parte di GitHub e garantire la corretta visualizzazione delle risorse.
- **Deployment**: Pubblicazione automatica dei file generati sul branch `gh-pages` del repository tramite l'azione
dedicata di _peaceiris_.

## Tracciamento e Reportistica
La gestione delle attività, la pianificazione del lavoro e il tracciamento dei progressi sono stati gestiti interamente
tramite **YouTrack** come piattaforma di project management e issue tracking.