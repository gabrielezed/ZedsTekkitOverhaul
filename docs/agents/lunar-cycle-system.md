# ZedsTekkitOverhaul — Lunar Cycle System

**Versione documento:** 1.0  
**Target:** Minecraft Java Edition 1.2.5 — Tekkit Classic  
**Destinatario:** LLM con accesso al workspace e agli agenti di sviluppo  
**Tipo:** Specifica funzionale, architetturale e di integrazione  
**Prima milestone:** Lunar Core + Moon Rendering + Event API  
**Stato:** Proposta progettuale da validare sul codice esistente

---

# 1. Visione e obiettivi

Implementare un sistema lunare che diventi una delle infrastrutture fondamentali della progressione survival di ZedsTekkitOverhaul.

Il ciclo lunare deve essere osservabile direttamente nel cielo, prevedibile dal giocatore e consultabile mediante API interne da altri moduli.

In futuro verrà utilizzato per:

- Attivare assedi durante determinate notti.
- Modificare la frequenza e la composizione degli spawn ostili.
- Gestire eventi speciali legati agli Enderman.
- Introdurre variazioni ambientali e fenomeni anomali.
- Coordinare l'evoluzione della minaccia nel mondo.
- Consentire l'implementazione di strumenti di previsione e difesa.

**Non implementare queste meccaniche nella prima milestone.**

L'obiettivo iniziale è fornire un'infrastruttura affidabile, testabile ed estensibile.

## 1.1 Principi architetturali

1. Il calendario è indipendente dal rendering.
2. Il calendario non dipende dal caricamento del client.
3. Il server determina lo stato autorevole.
4. Lo stato deve sopravvivere a riavvii e salvataggi.
5. Gli eventi devono essere emessi attraverso un'API dedicata.
6. Il sistema deve funzionare anche senza alcun listener registrato.
7. Non introdurre dipendenze obbligatorie da NEI, Inventory Tweaks o altri moduli non pertinenti.
8. Non modificare inutilmente la gestione vanilla di luce, tempo e spawn.
9. Le funzionalità devono rimanere compatibili con mondi preesistenti.
10. Tutti i comportamenti dipendenti dal tempo devono poter essere verificati tramite strumenti di debug.

---

# 2. Analisi preliminare obbligatoria

Minecraft 1.2.5 dispone già di otto fasi lunari.

L'agente deve esaminare il codice vanilla effettivamente presente nel workspace e individuare:

- Il metodo che calcola la fase lunare.
- La rappresentazione della luna nel rendering del cielo.
- La texture o l'atlante utilizzato.
- Il metodo che determina il giorno del mondo.
- La gestione del tempo totale e del tempo del giorno.
- I meccanismi di persistenza.
- I punti di sincronizzazione fra server e client.
- I comandi che modificano il tempo.
- Eventuali modifiche già applicate da altri moduli o mod.

Non presumere nomi di classi, metodi o campi: utilizzare quelli effettivamente presenti nella codebase decompilata.

## 2.1 Decisione sull'integrazione

Preferire questa strategia:

**Riutilizzare il rendering vanilla e sostituire o adattare soltanto la selezione della fase mostrata**, quando necessario.

Non introdurre un secondo renderer della luna se è possibile modificare correttamente il comportamento esistente.

Conservare:

- Posizione e movimento della luna nel cielo.
- Dimensioni coerenti con lo stile legacy.
- Illuminazione e blending originali.
- Compatibilità con texture pack legacy.

---

# 3. Modello del calendario lunare

## LUNAR-01 — Ciclo di otto fasi

**Priorità: P0**

Il sistema deve definire otto fasi lunari ordinate.

| Indice interno | Fase | Identificatore |
|---|---|---|
| 0 | Luna nuova | NEW_MOON |
| 1 | Falce crescente | WAXING_CRESCENT |
| 2 | Primo quarto | FIRST_QUARTER |
| 3 | Gibbosa crescente | WAXING_GIBBOUS |
| 4 | Luna piena | FULL_MOON |
| 5 | Gibbosa calante | WANING_GIBBOUS |
| 6 | Ultimo quarto | LAST_QUARTER |
| 7 | Falce calante | WANING_CRESCENT |

Questi indici sono una convenzione dell'API di ZedsTekkitOverhaul, non necessariamente quella del codice vanilla.

Implementare esplicitamente la corrispondenza con l'ordine delle texture e delle fasi native.

## LUNAR-02 — Durata e progressione

**Priorità: P0**

Valori predefiniti:

- 24.000 tick per giorno.
- 8 giorni per ciclo.
- 192.000 tick per ciclo completo.
- Una fase per giorno.
- Luna nuova come prima fase di un nuovo mondo.

Il sistema deve permettere di cambiare l'offset iniziale senza modificare la durata del giorno.

La durata di otto giorni è inizialmente fissa. Una futura modifica della durata del ciclo dovrà essere possibile senza riscrivere l'intero modulo.

Non è necessario rendere configurabile ogni parametro nella prima versione.

## LUNAR-03 — Origine temporale del calendario

**Priorità: P0**

Il calendario deve utilizzare una sorgente temporale autorevole e definita esplicitamente.

Proposta:

- Tempo totale del mondo come base logica.
- Offset lunare persistente per stabilire la prima fase.
- Calcolo deterministico della fase dal giorno logico.

Formula concettuale:

```java
logicalDay = floorDiv(effectiveTime, 24000L);
phaseIndex = floorMod(logicalDay + phaseOffset, 8);
```

Dove `phaseOffset` è scelto in modo che il primo giorno del nuovo mondo corrisponda a NEW_MOON.

Attenzione: la formula è concettuale. La corrispondenza fra tempo totale, tempo visualizzato e manipolazioni del calendario deve essere verificata nell'implementazione legacy.

Il giorno 0 è il primo giorno logico.

La luna piena si verifica durante il giorno logico 4, ossia nella quinta giornata.

### Requisito importante

Separare internamente:

- Tempo utilizzato per individuare la fase.
- Tempo del giorno utilizzato per stabilire alba, tramonto e notte.
- Eventuale tempo monotono necessario per cooldown e scheduling.

Non assumere che questi valori siano sempre equivalenti.

---

# 4. Gestione del tempo e casi particolari

## LUNAR-04 — Sonno

**Priorità: P0**

Dormire deve far avanzare il calendario coerentemente con il tempo effettivamente trascorso o saltato.

In questa milestone, il sonno vanilla resta consentito.

Il sistema deve:

- Ricalcolare la fase dopo il salto temporale.
- Emettere gli eventi appropriati.
- Evitare di duplicare transizioni.
- Non eseguire retroattivamente eventi di gameplay per ogni tick saltato.

Per ora non introdurre restrizioni al sonno.

In futuro il modulo degli assedi potrà impedire il sonno durante finestre specifiche attraverso un'integrazione separata.

## LUNAR-05 — Comandi di manipolazione del tempo

**Priorità: P0**

Definire esplicitamente come il calendario reagisce a operazioni equivalenti a:

- Aggiungere tempo.
- Impostare il momento della giornata.
- Avanzare di uno o più giorni.
- Tornare a una data precedente.

È necessario distinguere fra modifica del solo orario e modifica del giorno logico.

Comportamento desiderato:

- Spostamenti in avanti: aggiornare lo stato e segnalare le transizioni effettivamente attraversate.
- Spostamenti all'indietro: ricalcolare lo stato senza riattivare automaticamente eventi irreversibili.
- Grandi salti: non emettere migliaia di callback.
- Modifiche della sola ora: non alterare la fase per errore.

L'agente deve proporre un comportamento deterministico compatibile con le API temporali realmente disponibili.

## LUNAR-06 — Arresto e ripresa del mondo

**Priorità: P0**

Il calendario deve conservarsi correttamente attraverso:

- Salvataggio manuale.
- Salvataggio automatico.
- Chiusura del mondo.
- Riavvio del server.
- Riconnessione del client.
- Caricamento di un mondo esistente.

Nessuna transizione deve essere emessa nuovamente soltanto perché il mondo è stato ricaricato.

## LUNAR-07 — Mondi esistenti

**Priorità: P0**

I mondi creati prima dell'installazione del modulo devono essere supportati.

Proposta di migrazione iniziale:

- Non resettare il tempo del mondo.
- Inizializzare i dati mancanti.
- Allineare il calendario alla fase visualizzata dal vanilla, quando possibile.
- Registrare l'avvenuta inizializzazione.

Nei mondi nuovi utilizzare invece NEW_MOON come fase iniziale.

L'inizializzazione deve essere idempotente.

---

# 5. Rendering lunare

## RENDER-01 — Rendering delle otto fasi

**Priorità: P0**

La luna visibile nel cielo deve corrispondere sempre alla fase autorevole del calendario.

Requisiti:

- Otto fasi distinguibili.
- Corretto ordine delle fasi.
- Orientamento visivo coerente.
- Nessuna discontinuità durante il movimento nel cielo.
- Compatibilità con il ciclo giorno/notte vanilla.
- Nessuna texture placeholder permanente.

Utilizzare preferibilmente le risorse grafiche vanilla 1.2.5.

Le fasi possono cambiare istantaneamente al passaggio di giorno: non è necessaria un'interpolazione graduale fra le forme lunari.

## RENDER-02 — Movimento della luna

**Priorità: P0**

Conservare il movimento originale della luna.

Non modificare:

- Rotazione del cielo.
- Orbita apparente.
- Dimensioni angolari senza giustificazione.
- Posizione relativa alle stelle.
- Tempi di alba e tramonto.

Il calendario deve controllare la fase, non riscrivere la geometria del cielo.

## RENDER-03 — Texture legacy

**Priorità: P0**

Verificare il formato delle texture usato dalla build 1.2.5.

Se la texture vanilla è sufficiente, riutilizzarla.

Se è necessario fornire un atlante personalizzato:

- Conservare lo stile pixel art.
- Preferire risoluzione e proporzioni coerenti con il vanilla.
- Non introdurre dipendenze da shader.
- Evitare texture eccessivamente realistiche.
- Gestire correttamente filtri e coordinate UV.
- Garantire un comportamento prevedibile con texture pack non compatibili.

Non utilizzare texture moderne senza verificarne licenza, compatibilità e coerenza visiva.

## RENDER-04 — Compatibilità grafica

**Priorità: P1**

Testare:

- Fast/Fancy graphics, se applicabili.
- Risoluzioni e scale GUI differenti.
- Texture pack.
- Ambienti con prestazioni limitate.
- Cambiamento della fase durante il gioco.
- Riconnessione multiplayer.
- Nether ed End.

Il modulo non deve intervenire sul cielo delle altre dimensioni, salvo decisione esplicita futura.

## RENDER-05 — Nessuna alterazione dell'illuminazione

**Priorità: P0**

Non introdurre ancora variazioni di luce ambientale in funzione della fase lunare.

La luna piena potrà eventualmente influenzare la luminosità in una release successiva, ma il sistema base deve limitarsi al calendario e al rendering visivo.

---

# 6. Stato persistente e sincronizzazione

## STATE-01 — Persistenza per mondo

**Priorità: P0**

Persistenza logica consigliata:

```text
LunarWorldState
  schemaVersion
  initialized
  phaseOffset
  lastObservedLogicalDay
  lastDispatchedTransition
  cycleCount
  calendarConfigVersion
```

Questo è uno schema concettuale, non una struttura obbligatoria.

I dati derivabili dal tempo del mondo non devono essere duplicati inutilmente nella persistenza.

Memorizzare soltanto ciò che serve per:

- Configurazione.
- Migrazione.
- Recupero dello stato.
- Prevenzione delle transizioni duplicate.
- Eventuale tracciamento del ciclo.

Utilizzare il sistema di salvataggio già adottato dal progetto.

## STATE-02 — Server authoritative

**Priorità: P0**

Il server deve essere l'unica autorità sul calendario.

Il client può:

- Leggere lo stato sincronizzato.
- Calcolare anteprime visive.
- Renderizzare la luna.
- Mostrare informazioni nella GUI.

Il client non può:

- Decidere autonomamente transizioni di gameplay.
- Attivare eventi server.
- Alterare lo stato persistente.
- Forzare cambi di fase.

## STATE-03 — Sincronizzazione multiplayer

**Priorità: P0**

Sincronizzare lo stato almeno durante:

- Login.
- Cambio dimensione.
- Aggiornamento del calendario.
- Modifica amministrativa della fase.
- Riconnessione.

Evitare sincronizzazioni complete a ogni tick.

Preferire aggiornamenti su evento e riconciliazione quando necessario.

## STATE-04 — Dimensioni

**Priorità: P0**

Per ora esiste un calendario lunare globale riferito all'Overworld.

Le dimensioni alternative non hanno cicli indipendenti.

Le API dovranno comunque accettare un contesto mondo/dimensione per rendere possibile in futuro un comportamento specifico.

Le eventuali notifiche per gli assedi dovranno essere filtrabili per dimensione.

---

# 7. API interne pubbliche al progetto

## API-01 — LunarService

**Priorità: P0**

Definire un'interfaccia centrale per interrogare il calendario.

Esempio concettuale:

```java
public interface LunarService {
    LunarPhase getCurrentPhase(WorldContext world);
    LunarSnapshot getSnapshot(WorldContext world);

    long getLogicalDay(WorldContext world);
    long getCycleIndex(WorldContext world);

    boolean isFullMoon(WorldContext world);
    boolean isNewMoon(WorldContext world);

    long getDaysUntilPhase(
        WorldContext world,
        LunarPhase phase
    );
}
```

I nomi sono indicativi e devono essere adattati al codice reale.

La consultazione del calendario non deve modificare lo stato.

## API-02 — LunarPhase

**Priorità: P0**

Definire una rappresentazione stabile delle otto fasi.

Proprietà minime:

- ID stabile.
- Nome localizzabile.
- Indice nella sequenza.
- Eventuale valore indicativo di illuminazione.
- Identificatore grafico per la texture.
- Indicazione di luna nuova/piena.

Non associare direttamente alla fase una difficoltà di combattimento obbligatoria.

## API-03 — LunarSnapshot

**Priorità: P0**

Restituire un oggetto immutabile che rappresenta uno stato coerente del calendario.

Informazioni consigliate:

```text
LunarSnapshot
  phase
  logicalDay
  cycleIndex
  dayInCycle
  timeOfDay
  isNight
  ticksUntilNextPhase
  dimensionId
```

Lo snapshot deve poter essere condiviso senza che il chiamante possa alterare lo stato.

## API-04 — Event Bus

**Priorità: P0**

Predisporre un sistema di eventi senza dipendenze dirette da orde o mob.

Eventi iniziali:

```text
LunarPhaseChangedEvent
LunarDayStartedEvent
LunarNightStartedEvent
LunarNightEndedEvent
LunarCycleCompletedEvent
LunarCalendarResyncedEvent
```

Ogni evento deve contenere il contesto necessario.

Per esempio:

```text
LunarPhaseChangedEvent
  world
  previousPhase
  currentPhase
  logicalDay
  cycleIndex
  transitionReason
```

Valori possibili di `transitionReason`:

```text
NATURAL
TIME_SKIP
ADMIN_COMMAND
CALENDAR_RESET
WORLD_INITIALIZATION
```

L'inizializzazione e la risincronizzazione devono essere distinguibili da una normale transizione.

### Semantica degli eventi

- Gli eventi ordinari vengono emessi dal server.
- Gli eventi non devono essere duplicati.
- Gli eventi non devono essere emessi a ogni tick.
- Un listener non deve modificare direttamente lo stato interno del calendario.
- Un listener difettoso non deve corrompere il calendario.
- Gli eventi non rappresentano necessariamente operazioni che possono essere annullate.
- Le notifiche di sincronizzazione non devono essere trattate come nuove notti.

## API-05 — Listener registration

**Priorità: P0**

Predisporre un meccanismo per registrare e rimuovere listener.

Deve permettere ai moduli futuri di reagire alle transizioni senza modificare il LunarService.

Esempio di utilizzo futuro:

```java
lunarEvents.register(listener);
```

I dettagli devono rispettare l'architettura esistente.

Prevedere un ciclo di vita chiaro per evitare listener duplicati dopo il caricamento di più mondi.

---

# 8. API per gli assedi futuri

**Questa sezione definisce esclusivamente contratti di integrazione.**

Non implementare ancora spawning, AI, orde o combattimento.

## SIEGE-API-01 — LunarEventWindow

**Priorità: P0 — Contratto**

Definire un modello generale di finestra temporale.

Campi concettuali:

```text
LunarEventWindow
  id
  phasePredicate
  startTimeOfDay
  endTimeOfDay
  dimensionPolicy
  priority
```

La finestra rappresenta una condizione temporale, non un evento di gameplay completo.

Esempi futuri:

- Finestra di assedio durante la luna piena.
- Finestra di pattugliamento in fase crescente.
- Finestra di anomalie durante la luna nuova.

## SIEGE-API-02 — LunarEventPolicy

**Priorità: P0 — Contratto**

Predisporre un'interfaccia per determinare se una particolare condizione temporale è soddisfatta.

Esempio:

```java
public interface LunarEventPolicy {
    boolean matches(LunarSnapshot snapshot);
}
```

Questo permette a futuri sistemi di definire le proprie regole senza introdurre dipendenze nel LunarService.

## SIEGE-API-03 — MoonActivityProfile

**Priorità: P1 — Contratto dati**

Predisporre una struttura opzionale per fornire informazioni generiche sull'attività associata alle fasi.

Possibili campi:

```text
MoonActivityProfile
  phase
  activityCategory
  relativeIntensity
  modifiers
```

Non interpretare questi valori come modificatori diretti della difficoltà.

Non alterare automaticamente gli spawn vanilla.

I valori effettivi saranno stabiliti quando verrà progettato il sistema delle orde.

## SIEGE-API-04 — Separazione fra calendario e minaccia

**Priorità: P0**

Il LunarService non deve conoscere:

- Numero di mob.
- Tipologia di nemici.
- Posizione della base del giocatore.
- Livello tecnologico.
- Progressione narrativa.
- Resistenza dei blocchi.
- Stato delle difese.
- Composizione delle ondate.

Queste informazioni apparterranno a sistemi separati.

La luna deve definire il contesto temporale, non la logica degli assedi.

## SIEGE-API-05 — Future SpawnDirector

**Priorità: P0 — Solo punto di estensione**

Progettare il calendario affinché in futuro sia utilizzabile da un modulo concettualmente denominato `SpawnDirector`.

Il futuro sistema potrà ricevere:

- Fase corrente.
- Stato della notte.
- Ciclo corrente.
- Finestre attive.
- Informazioni di progressione esterne.
- Contesto della dimensione.

Non creare un'implementazione fittizia complessa del direttore degli spawn.

È sufficiente predisporre le interfacce necessarie.

---

# 9. API per la progressione futura

## PROG-01 — Ciclo e livello di minaccia

**Priorità: P1 — Predisposizione**

Il calendario deve rendere disponibile il numero di cicli completati.

Tuttavia:

**Il numero di cicli non equivale al livello di difficoltà.**

In futuro un altro componente potrà calcolare:

```text
ThreatLevel = f(
    lunarCycle,
    playerProgression,
    worldEvents,
    localEndActivity
)
```

Questa formula è esclusivamente concettuale.

Non introdurre adesso il sistema di ThreatLevel.

## PROG-02 — Possibilità di calendari speciali

**Priorità: P2 — Solo estensibilità**

Non escludere la futura introduzione di:

- Lune anomale.
- Eclissi.
- Lune associate all'End.
- Eventi narrativi che modificano temporaneamente il calendario.

Evitare però di progettare un sistema eccessivamente astratto per funzionalità non ancora definite.

Il modello delle otto fasi ordinarie rimane la base.

---

# 10. Strumenti di debug

## DEBUG-01 — Ispezione dello stato

**Priorità: P0**

Aggiungere strumenti di debug per leggere:

- Fase attuale.
- Giorno logico.
- Indice del ciclo.
- Tempo fino alla prossima fase.
- Stato di sincronizzazione.
- Ultima transizione registrata.

Preferire un comando amministrativo o una funzione già coerente con i sistemi di debug del progetto.

## DEBUG-02 — Simulazione del calendario

**Priorità: P0**

Consentire agli sviluppatori di:

- Avanzare di una fase.
- Impostare una fase specifica.
- Avanzare di un ciclo.
- Ripristinare il calendario.
- Simulare un salto temporale.

I comandi devono essere limitati agli ambienti o ai permessi appropriati.

Le operazioni devono passare attraverso il LunarService, evitando modifiche dirette e incoerenti alla persistenza.

## DEBUG-03 — Event logging

**Priorità: P1**

Aggiungere logging configurabile per:

- Transizioni.
- Sincronizzazioni.
- Salvataggi.
- Migrazioni.
- Errori dei listener.

Non generare log continui a ogni tick.

## DEBUG-04 — Listener dimostrativo

**Priorità: P0**

Implementare un listener di debug minimale che reagisca, per esempio, all'inizio di una notte di luna piena.

Il listener può scrivere un messaggio nel log o mostrare una notifica esclusivamente in modalità debug.

Serve a verificare l'intero percorso:

Calendario → Evento → Listener.

Non deve introdurre comportamenti gameplay permanenti.

---

# 11. Performance e sicurezza

## PERF-01 — Aggiornamenti

**Priorità: P0**

Non ricalcolare strutture complesse a ogni tick.

Il calcolo della fase deve essere economico.

Le transizioni devono essere elaborate soltanto quando necessario.

Il rendering non deve interrogare strutture server non disponibili sul client.

## PERF-02 — Nessuna scansione del mondo

**Priorità: P0**

Il calendario non deve:

- Scansionare chunk.
- Cercare entità.
- Individuare basi.
- Calcolare distanze dai giocatori.
- Modificare blocchi.
- Gestire direttamente spawn.

Queste operazioni saranno responsabilità di sistemi futuri.

## PERF-03 — Resilienza

**Priorità: P0**

Il sistema deve funzionare anche quando:

- Nessun listener è registrato.
- Un client si collega a calendario già avanzato.
- Il server esegue un salto temporale.
- Il rendering non è disponibile.
- Il mondo viene salvato durante una transizione.
- I dati persistenti sono assenti o appartengono a una versione precedente dello schema.

Gestire esplicitamente gli stati iniziali e le condizioni anomale.

---

# 12. Test di accettazione

## TEST-01 — Nuovo mondo

Creare un nuovo mondo.

Risultato atteso:

- Il primo giorno corrisponde a luna nuova.
- Il calendario parte dal primo ciclo.
- Nessun errore di rendering.
- Lo stato viene salvato correttamente.

## TEST-02 — Otto fasi

Avanzare di otto giorni.

Risultato atteso:

- Tutte le fasi vengono attraversate in ordine.
- La luna visibile corrisponde allo stato.
- Il ciclo si ripete correttamente.

## TEST-03 — Prima luna piena

Avanzare fino alla quinta giornata.

Risultato atteso:

- Fase FULL_MOON.
- Texture corretta.
- Evento di transizione emesso una sola volta.

## TEST-04 — Sonno

Dormire ripetutamente.

Risultato atteso:

- Il calendario avanza correttamente.
- Nessuna transizione duplicata.
- Nessuna incoerenza fra server e client.

## TEST-05 — Riavvio

Salvare e riaprire il mondo.

Risultato atteso:

- Stesso giorno logico.
- Stessa fase lunare.
- Stesso indice di ciclo.
- Nessuna transizione artificiale.

## TEST-06 — Multiplayer

Collegare almeno due client.

Risultato atteso:

- Stessa fase.
- Stesso calendario.
- Nessuna divergenza persistente.
- Corretta sincronizzazione all'accesso.

## TEST-07 — Cambio dimensione

Entrare nel Nether e tornare.

Risultato atteso:

- Calendario globale invariato.
- Nessun ciclo separato accidentale.
- Corretta rappresentazione al ritorno.

## TEST-08 — Salto temporale

Utilizzare i comandi di debug.

Risultato atteso:

- Fase aggiornata.
- Eventi coerenti.
- Nessuna emissione incontrollata di callback.

## TEST-09 — Mondo legacy

Aprire un mondo creato prima della modifica.

Risultato atteso:

- Migrazione non distruttiva.
- Tempo preesistente conservato.
- Calendario inizializzato.
- Nessun crash.

## TEST-10 — API future

Registrare un listener temporaneo che riconosca una luna piena.

Risultato atteso:

- Evento ricevuto.
- Dati corretti.
- Nessuna dipendenza da sistemi di spawn.
- Nessun mob generato automaticamente.

---

# 13. Piano di sviluppo

## Milestone A — Audit e calendario

- Analisi del sistema vanilla.
- Individuazione della sorgente temporale.
- Modellazione delle fasi.
- LunarService.
- Calcolo del calendario.
- Persistenza.
- Test logici.

## Milestone B — Rendering

- Integrazione con renderer esistente.
- Mappatura delle otto texture.
- Verifica visiva.
- Compatibilità con texture pack.
- Test client.

## Milestone C — Multiplayer

- Stato autorevole server.
- Sincronizzazione.
- Login e riconnessione.
- Cambio dimensione.
- Test multiplayer.

## Milestone D — Event API

- Eventi lunari.
- Listener registration.
- Event windows.
- Test delle transizioni.
- Listener di debug.

## Milestone E — Hardening

- Gestione del sonno.
- Salti temporali.
- Migrazione mondi legacy.
- Test di persistenza.
- Documentazione delle API interne.
- Verifica regressioni.

---

# 14. Definition of Done

La prima milestone è completata quando:

1. Il calendario lunare funziona su mondi nuovi ed esistenti.
2. Le otto fasi sono correttamente rappresentate.
3. Un mondo nuovo inizia dalla luna nuova.
4. Il calendario sopravvive ai riavvii.
5. Il multiplayer mantiene uno stato coerente.
6. I salti temporali non generano duplicazioni.
7. Gli altri moduli possono interrogare il calendario.
8. Gli altri moduli possono ascoltare gli eventi.
9. Esiste un punto di integrazione per le future finestre di assedio.
10. Non è stato introdotto alcun nuovo spawn o comportamento ostile.
11. Il sistema non altera il normale gameplay quando non sono presenti listener.
12. Il codice è accompagnato da test e documentazione adeguati.

**Principio conclusivo:** il sistema lunare deve essere un servizio infrastrutturale del mondo, non una funzionalità monolitica che contiene già tutta la logica degli assedi.

La luna stabilisce quando accadono le cose. I moduli futuri stabiliranno cosa accade, dove e con quale intensità.