---
title: Introduzione
nav_order: 1
parent: Report
---
# Introduzione
Il progetto SplagueNet si pone l’obiettivo di realizzare un simulatore in Scala 3, ispirato alle meccaniche di gioco di
_Plague Inc._, per modellare e studiare la propagazione di malware all'interno di topologie di rete definite dall'utente, adottando i principi della programmazione funzionale.

## Dinamiche dell'infezione su rete
A differenza dei tradizionali sistemi di sicurezza basati su firme o analisi statiche, SplagueNet adotta un approccio dinamico.
La rete viene modellata come un grafo in cui i nodi rappresentano dispositivi (es. workstation, server, router) e gli archi
rappresentano i canali di comunicazione. Il malware si diffonde attraverso la rete sfruttando vulnerabilità locali, muovendosi
da un nodo all'altro in base a tratti evolutivi configurabili (es. infectivity, stealth, payload severity, etc...).
Parallelamente, il sistema reagisce attivando contromisure automatizzate al superamento di determinate soglie di
awareness (consapevolezza del rischio).


## Finalità del simulatore
SplagueNet si propone di costruire un ambiente flessibile e componibile per valutare l'interazione tra strategie offensive
(il malware e le sue caratteristiche) e difensive (le contromisure di rete).

Le astrazioni fondamentali offerte dal simulatore sono:

- **Network Topology**: la struttura a grafo che definisce la connettività e le dipendenze tra i nodi della rete.
- **Malware/Virus**: un'entità caratterizzata da un insieme di tratti che ne definiscono la probabilità d'infezione, la
  velocità di propagazione, la tracciabilità e l'impatto causato sulla topologia.
- **Countermeasures**: un insieme di contromisure reattive (es. patching d'emergenza, isolamento
  dei nodi critici, attivazione firewall) che si attivano dinamicamente al variare dell'indice di awareness globale del sistema.
- **Scenario**: entità che rappresenta uno scenario di esecuzione, formata dalla topology, malware e set di contromisure
  con la relativa configurazione.

L'obiettivo principale dell'osservatore è analizzare l'esito della simulazione (compromissione totale del sistema vs.
contenimento) in funzione della topologia di rete, dei tratti del malware e delle policy di difesa adottate.

## Guida alla lettura
Il testo è organizzato per guidare il lettore attraverso le diverse fasi ingegneristiche del progetto SplagueNet
. I capitoli successivi affronteranno i seguenti temi:
- [Processo di sviluppo](processo_di_sviluppo.md)
- [Specifica dei requisiti](specifica_dei_requisiti.md)
- [Design Architetturale](design_architetturale.md)
- [Design di Dettaglio](design_di_dettaglio.md)
- [Implementazione](implementazione.md)
- [Testing](testing.md)
- [Retrospettiva](retrospettiva.md)