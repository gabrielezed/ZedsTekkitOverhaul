# ZedsTekkitOverhaul — Project Vision & Game Design Charter

> **Documento guida permanente per sviluppatori e agenti LLM.**  
> **Revisione progettuale:** 12 novembre 2026.  
> **Target:** Minecraft Java Edition 1.2.5, Tekkit Classic 3.1.2.  
> **Scopo:** descrivere *perché* il progetto esiste, quale esperienza deve offrire e quali limiti non deve oltrepassare. Le specifiche delle singole feature definiscono il *come*; il codice e le prove runtime definiscono ciò che è effettivamente implementato.
>
> **Legenda normativa:**
> - **[DIREZIONE APPROVATA]**: scelta di prodotto confermata dal project owner; vincola nuove proposte, salvo revisione esplicita.
> - **[PROPOSTA DA VALIDARE]**: soluzione promettente, non ancora approvata come comportamento definitivo.
> - **[DECISIONE APERTA]**: domanda sostanziale che gli agenti non devono risolvere autonomamente introducendo nuove meccaniche.
> - **[PROTOTIPO SOTTO REVISIONE]**: codice o contenuto sperimentale che non costituisce un impegno per la release finale.
> - **[STATO IMPLEMENTATIVO]**: fotografia informativa, soggetta a verifica nel repository; non sostituisce test e documentazione tecnica.
>
> **Precedenza:** visione di prodotto e decisioni approvate > proposte e default provvisori > preferenze implementative dell'agente. Non interpretare una feature citata in questo file come autorizzazione a implementarla immediatamente.

---

## 1. Manifesto

**ZedsTekkitOverhaul vuole essere la Tekkit classica che ricordiamo, ma inserita in un Minecraft più vivo, ostile, misterioso e degno della potenza che il giocatore può raggiungere.**

Il cuore del progetto non è la complessità tecnologica. È la libertà di costruire, esplorare, sperimentare, difendersi e diventare straordinariamente potenti. IC2, RedPower, BuildCraft, EE2 e le eventuali aggiunte ZTO devono offrire strumenti per affrontare un mondo più significativo, non diventare il contenuto principale per il solo fatto di esistere.

Una piccola farm intelligente deve essere una scelta legittima; una fabbrica enorme può essere un'opportunità, mai un pedaggio obbligatorio. Il giocatore che conosce Tekkit Classic deve riconoscerla immediatamente e trovare nuove ragioni per usare ciò che già conosce.

**[DIREZIONE APPROVATA] ZTO è un overhaul selettivo, non un'espansione quantitativa.** Non aggiungere 200 materiali, cento macchinari e nuove catene produttive soltanto per aumentare la durata o il volume del contenuto. Ogni novità deve aprire una possibilità significativa di gameplay, interazione o narrazione ambientale; preferire la reinterpretazione di sistemi esistenti quando è sufficiente.

### Promessa al giocatore

- Puoi iniziare senza conoscere a fondo ogni mod tecnica. Il gioco suggerisce possibilità, non impone una sequenza di quest.
- La casa improvvisata può bastare all'inizio; minacce leggibili e progressivamente articolate danno motivi reali per costruire e migliorare una fortezza.
- Esplorare luoghi nuovi può essere pericoloso, utile e misterioso; prepararsi, ritirarsi e tornare sono strategie legittime.
- Puoi rispondere alle sfide con combattimento, costruzione, equipaggiamento, ingegno, alchimia, automazione oppure combinazioni di questi approcci.
- La potenza avanzata deve essere percepibile: QuantumSuit, Red Matter Armor e industria matura permettono di dominare rischi prima importanti.
- La progressione verso l'End nasce da scoperte e fenomeni nel mondo, non da una lunga lista di ricette o miniboss obbligatori.
- Il gioco conserva il senso di libertà, familiarità e mistero del Minecraft classico.

## 2. Identità, selettività e controllo dello scope

### Siamo [DIREZIONE APPROVATA]

- Una **sandbox survival-industriale**, non un percorso lineare di crafting.
- Una reinterpretazione rispettosa di **Minecraft 1.2.5 e Tekkit Classic**, con estetica e interazioni riconoscibili.
- Un **overhaul integrato**: il mondo dà nuovi significati alle mod già presenti.
- Un'esperienza **technical-light-friendly**: profondità disponibile, complessità tecnica non obbligatoria.
- Un gioco **atmosferico e sistemico**: il mondo reagisce, il giocatore osserva, interpreta e decide.

### Non siamo [DIREZIONE APPROVATA]

- Un quest pack o una progressione a ricette forzate.
- Un GregTech più accessibile, un kitchen sink o una collezione di prototipi senza una funzione comune.
- Un survival di mob con HP e danno progressivamente moltiplicati.
- Un mondo che punisce la lentezza, la creatività edilizia o le fabbriche grandi per il solo fatto di esistere.
- Un'esperienza che richiede una base di forma predeterminata, marker di proprietà, macchine obbligatorie o percorsi unici.
- Una Twilight Forest con una sequenza esplicita di miniboss, barre della salute e ricompense obbligate.

### Politica dei prototipi [DIREZIONE APPROVATA]

**Il codice realizzato non conferisce automaticamente a una feature il diritto di rimanere nel prodotto.** I prototipi sono esperimenti, anche quando la loro implementazione è costata molto lavoro.

- **Distillazione frazionata:** [PROTOTIPO SOTTO REVISIONE]. La sua permanenza nell'esperienza finale è esplicitamente incerta.
- **Advanced Crafting Table e crafting 5×5:** [PROTOTIPO SOTTO REVISIONE]. Il progetto non deve progettare nuove catene di ricette intorno alla griglia 5×5 come se fosse un requisito approvato.
- **Foundry e altre grandi architetture industriali:** il valore va verificato rispetto al gameplay, non presunto dalla complessità tecnica.
- Non cancellare silenziosamente blocchi, TileEntity, ID o NBT già presenti nei salvataggi. L'eventuale rimozione richiede un piano di compatibilità, migrazione o dismissione deliberata.

**Filtro di prodotto:** una feature deve rispondere alla domanda «Quale esperienza non potremmo ottenere in modo soddisfacente usando già Minecraft e Tekkit?». Una nuova macchina è giustificata quando offre una capacità concreta: per esempio un compressore IC2 che ricarica bombole e rende praticabile l'esplorazione subacquea.

## 3. Feeling, atmosfera e narrazione

### Familiarità prima della novità [DIREZIONE APPROVATA]

Il primo impatto deve ricordare Minecraft 1.2.5. Interfacce sobrie, esplorazione libera, strutture di blocchi concreti, scoperte spontanee e nessuna esposizione narrativa obbligatoria. Nuove meccaniche devono sembrare evoluzioni naturali del mondo, non sistemi importati da un altro modpack.

### Tensione intermittente [DIREZIONE APPROVATA]

Il mondo alterna **calma → esplorazione → presagi → preparazione → pericolo → recupero**. La difficoltà non è una pressione costante. Le notti ordinarie non devono equivalere tutte alla luna piena, ma neppure essere universalmente irrilevanti.

### Narrazione non narrata [DIREZIONE APPROVATA]

Preferire fenomeni, comportamenti dei mob, anomalie ambientali, rovine, trasformazioni locali, tracce e conseguenze osservabili. Il giocatore collega indizi senza che il gioco debba annunciare «evento iniziato», «mini-boss» o «missione completata».

**Gli eventi possono essere rigorosamente codificati internamente e restare misteriosi nell'interfaccia normale.** Gli strumenti diagnostici devono invece consentire agli sviluppatori di comprenderne cause, limiti e stato.

Evitare esposizioni obbligatorie, lunghe sequenze di dialoghi, quest marker e barre boss introdotte soltanto per nominare un incontro.

### Mistero dell'End [DIREZIONE APPROVATA]

Gli Enderman e le anomalie nell'Overworld costituiscono un filo conduttore verso l'End, ma non devono essere spiegati completamente. Il drago è il punto di convergenza tematico, con un omaggio al finale originale del gioco.

## 4. Pilastri di game design

### 4.1 Libertà di approccio [DIREZIONE APPROVATA]

La progressione può essere difficile e contenere luoghi praticamente proibitivi senza preparazione; non deve prescrivere un'unica armatura, macchina o ordine di attività. Per ogni ostacolo importante valutare più soluzioni ragionevoli: combattimento, equipaggiamento, costruzione, automazione, vie alternative, ingegneria ambientale, preparazione o ritirata.

**Attenzione:** più di un percorso non significa necessariamente che ogni risorsa sia disponibile ovunque. Una scoperta o una regione speciale può restare importante senza trasformare il gioco in una checklist.

### 4.2 Tecnologia come mezzo [DIREZIONE APPROVATA]

Le macchine risolvono problemi e aprono possibilità: difesa, approvvigionamento, mobilità, esplorazione, ossigeno, energia, logistica e produzione utile. Non introdurre una nuova tecnologia soltanto per trasformare materiali già ottenibili in un ingrediente intermedio.

### 4.3 Fortezza e costruzione [DIREZIONE APPROVATA]

La base fortificata è una possibilità centrale, non un obbligo formale. Castelli, bunker, case sugli alberi, caverne adattate, laboratori all'aperto e avamposti lontani devono essere tutti legittimi.

Gli accessi, le barriere, i fossati, la posizione delle macchine, la luce, le trappole e i percorsi difensivi devono modificare realmente gli incontri. La protezione non deve dipendere esclusivamente dall'aver disseminato torce.

### 4.4 Difficoltà significativa [DIREZIONE APPROVATA]

Difficoltà significa decisioni, rischio, tattica, conoscenza e adattamento, non spesa artificiale di risorse, distruzione indiscriminata o moltiplicatori infiniti di salute. Il giocatore avanzato deve diventare davvero potente; i problemi late game possono cambiare natura anziché limitarsi a numeri più grandi.

### 4.5 Scarsità significativa [DIREZIONE APPROVATA]

Le risorse che rappresentano scoperte eccezionali possono essere intenzionalmente rare, non trasmutabili e associate a luoghi o eventi. La scarsità deve promuovere l'esplorazione, non il grinding ripetitivo né una catena di macchine obbligatorie.

## 5. Loop di gioco e progressione

**Esplora → scopri rischi e risorse → prepara rifugi/equipaggiamento → costruisci o migliori difese e produzione → affronta nuovi pericoli → ottieni possibilità → esplora ancora.**

Questa sequenza descrive una dinamica emergente, non una quest line. Un giocatore può costruire molto, esplorare a lungo, usare poche macchine o privilegiare il combattimento.

- **Inizio:** rifugio, sopravvivenza familiare, prime notti diversificate e primi indizi anomali.
- **Early-mid:** base e logistica più robuste, pattuglie, mobilità, esplorazione rischiosa ma accessibile.
- **Mid:** difese e strumenti più sofisticati, luoghi insoliti, minacce con ruoli tattici.
- **Late:** equipaggiamento potente, territori anomali, capacità di controllare il rischio e preparazione all'End.
- **Endgame:** accesso all'End fondato su esplorazione significativa e affrontamento del drago rielaborato.

Queste etichette non sono livelli minimi rigidi e non autorizzano blocchi artificialmente «vietati finché non hai sbloccato X».

## 6. Mondo, biomi pericolosi e nemici

### Regioni pericolose [DIREZIONE APPROVATA]

Il mondo deve contenere luoghi che suggeriscano cautela e richiedano preparazione. Gli ostacoli devono essere leggibili, diegetici e aggirabili con ingegno, non muri invisibili di livello.

### Regione sottomarina anomala [PROPOSTA DA VALIDARE]

Direzione creativa proposta dal project owner: una grande regione sottomarina con fauna ostile, possibili Enderman adattati all'ambiente e risorse/scoperte rilevanti anche per l'endgame.

- **Ossigeno:** bombole con riserva comprensibile e un compressore alimentato tramite IC2 per ricaricarle.
- **Logistica:** possibilità di organizzare un piccolo punto di rifornimento presso il luogo d'esplorazione; non imporre necessariamente una grande fabbrica.
- **Equipaggiamento:** valutare integrazione con armature esistenti e avanzate, evitando di invalidare automaticamente la QuantumSuit.
- **Contenimento dello scope:** non aggiungere di default simulatori completi di pressione, decompressione, temperatura o altri bisogni fisiologici.
- **Generazione:** [DECISIONE APERTA] vero bioma, struttura/regione speciale o altra soluzione compatibile con mondi nuovi ed esistenti.

La bombola e il compressore sono esempi approvati come *motivazione di gameplay* per nuove tecnologie, non una specifica già definita.

### Regione enderica distorta superficiale [PROPOSTA DA VALIDARE]

Una regione dell'Overworld molto ostile, indicativamente mid/late game, caratterizzata da ambiente, segnali e comportamenti enderici anomali. Deve evocare l'End senza spiegarne tutto, offrendo scoperte utili all'endgame.

Non ridurla a un normale bioma con zombie più resistenti. Preferire teletrasporto, osservazione, alterazioni ambientali, rischi di posizione e incontri inconsueti.

### Altre grandi regioni [DECISIONE APERTA]

Il numero di regioni speciali (per esempio quattro o cinque), le loro identità, l'ordine di scoperta e i materiali associati non sono definiti. Zone glaciali, paludi contaminate e altre idee precedenti restano ipotesi, non impegni.

### Enderman [DIREZIONE APPROVATA + PROPOSTE]

Ampliare il loro ruolo nel mondo: apparizioni insolite, osservazione, interferenze mirate, spostamento di blocchi, fenomeni locali e talvolta attacchi coordinati.

Archetipi come stalker, razziatore, cacciatore, sabotatore e varianti adattate a specifiche regioni rimangono [PROPOSTA DA VALIDARE]. Non trasformare ogni variante in semplice aumento di HP.

### Ruoli ostili [PROPOSTA DA VALIDARE]

Fanteria, assaltatori, aggiratori, supporti e sabotatori possono creare contromisure edilizie e tattiche differenti. Ogni comportamento significativo deve poter essere osservato e contrastato, senza distruggere gratuitamente le strutture.

### Griefing controllato [DIREZIONE APPROVATA]

Minacce speciali possono interferire con difese e infrastrutture, ma evitare la distruzione irreversibile arbitraria di macchine costose e ore di costruzione. Distinguere danni recuperabili, conseguenze rilevanti e perdita permanente. L'eventuale sabotaggio deve essere anticipabile e tecnicamente limitato.

## 7. Calendario lunare, notte e incontri ostili

### Calendario [DIREZIONE APPROVATA]

La luna scandisce un ritmo naturale di rischio. Il sistema corrente utilizza otto fasi in otto giorni Minecraft; i nuovi mondi sono allineati alla luna nuova, e la prima luna piena corrisponde al **quinto giorno di gioco** contando il giorno iniziale come primo (indice logico 4). L'ottavo giorno è luna calante, non luna piena.

Non modificare l'allineamento o la persistenza dei mondi senza una decisione esplicita e una migrazione verificabile.

### Difficoltà durante l'intero ciclo [DIREZIONE APPROVATA]

**La luna piena è il picco, non l'unica notte pericolosa.** Le altre fasi devono contribuire a un'esperienza più ostile e variata rispetto a Minecraft/Tekkit originali, ma conservare periodi di recupero.

Il calendario determina *quando* un incontro può essere eleggibile. Altri sistemi stabiliranno *se*, *dove*, *con quali mob*, *con quali equipaggiamenti* e *con quale intensità*. Non usare la fase come un moltiplicatore automatico e universale di HP o danno.

**[IMPLEMENTATION STATUS]** The full-moon PLAYER_ASSAULT is implemented. PR #25 added first-quarter and waxing-gibbous patrols in alpha.4. PR #26 added bounded encounter loadouts in alpha.5. Alpha.6 introduces outdoor placement in artificial light with local approach checks. Territorial threat, persistent settlements and SITE_SIEGE remain future milestones. [Patrol rules](docs/agents/lunar-pressure.md) · [Encounter loadouts](docs/agents/survival-alpha5.md) · [Outdoor placement and evidence](docs/agents/survival-alpha6.md)

### Sonno e letto [DIREZIONE APPROVATA SULL'INTENTO; PROTOTIPO SUL COMPORTAMENTO]

Il letto deve conservare la funzione di **punto di respawn**. Il project owner è orientato a **rimuovere completamente il salto della notte tramite sonno**, affinché il ciclo di rischio non sia eludibile con un click.

L'obiettivo è approvato come direzione progettuale; la semantica tecnica e il bilanciamento definitivo richiedono un prototipo verificato. La notte non deve obbligare a combattere continuamente: un rifugio e attività produttive tranquille devono restare possibilità valide.

**[DECISIONI APERTE]** Configurabilità per mondo, comportamento del letto in condizioni speciali, compatibilità con vecchi salvataggi, eventuali altre forme di avanzamento temporale, assenza del giocatore e futuro multiplayer. Non introdurre sistemi di fatica o bisogno di dormire senza approvazione.

### PLAYER_ASSAULT e SITE_SIEGE [DIREZIONE APPROVATA]

- **PLAYER_ASSAULT:** incontro temporaneo diretto alle vicinanze del giocatore, indipendente dal riconoscimento di una casa. Può avvenire durante esplorazione o presso una fortificazione.
- **SITE_SIEGE:** futuro assedio a un luogo persistente riconosciuto come insediamento/avamposto. Non è un PLAYER_ASSAULT rinominato quando il giocatore è a casa.

Non introdurre marker obbligatori, claim, letti o macchine richiesti per dichiarare una base. Il luogo bersaglio può restare significativo indipendentemente dalla posizione corrente del giocatore.

## 8. Luce, spawning e ambienti protetti

### Sicurezza non riducibile alle torce [DIREZIONE APPROVATA]

**Una distesa all'aperto illuminata da torce non dovrebbe diventare automaticamente immune a tutte le minacce ostili.** La luce deve restare utile per visibilità e difesa, ma non essere l'unico modo di garantire la sicurezza.

La costruzione deve acquisire valore: muri, accessi, coperture, fossati, distanze e ostacoli influenzano il rischio.

### Regole di spawning [PROPOSTA DA VALIDARE]

Distinguere almeno:

- **Esterno illuminato:** possibile presenza di alcuni incontri o ruoli speciali, anche se la luce ordinaria scoraggerebbe spawn vanilla.
- **Interno protetto:** evitare spawn arbitrari all'interno di rifugi validi.
- **Grotta naturale:** non considerarla automaticamente un rifugio solo perché coperta.
- **Caverna adattata a base:** permettere che possa diventare spazio protetto attraverso modifiche, accessi e uso reale.
- **Perimetro:** i nemici possono avvicinarsi dall'esterno; non devono teletrasportarsi dentro per aggirare una buona architettura soltanto per soddisfare un budget.

**[DECISIONE APERTA]** Quali spawn vanilla, lunari o speciali debbano ignorare parzialmente la luce; come determinare il livello di protezione; quali condizioni rendano un interno sicuro; come mantenere prestazioni e compatibilità legacy.

Non implementare un classificatore universale di edifici come semplice correzione a una pattuglia che non riesce a generare mob. Preferire prototipi piccoli e misurabili, mentre la riconoscibilità territoriale viene sviluppata nella propria milestone.

## 9. Attività territoriale, provenienza dei blocchi e insediamenti

### Minaccia territoriale [DIREZIONE APPROVATA]

Il rischio futuro potrà dipendere dall'attività locale, dal contesto ambientale e dalla progressione significativa, entro limiti, saturazione e decadimento. Non aumentare indefinitamente la difficoltà perché il giocatore rimane in casa o possiede una fabbrica enorme.

- **Territorial Activity:** registra in modo parsimonioso visite e presenza, senza dichiarare una base.
- **Threat Assessment:** combina informazioni territoriali e pericolo ambientale, mantenendo un'intensità locale limitata.
- **Settlement Recognition:** interpreta segnali di attività e costruzione per riconoscere insediamenti senza forma prescritta.
- **Encounter Director:** usa i segnali disponibili per scegliere incontri; SITE_SIEGE avrà un bersaglio diverso da PLAYER_ASSAULT.

### Provenienza delle costruzioni [DIREZIONE APPROVATA COME SEGNALE DA STUDIARE]

Il riconoscimento degli ambienti chiusi e delle basi deve valutare **anche, e potenzialmente soprattutto, se il territorio è stato modificato dal giocatore**, non soltanto forma e geometria.

Minecraft 1.2.5 non conserva una provenienza universale, persistente e interrogabile per ogni blocco. Il tracciamento richiederebbe una soluzione ZTO dedicata e un audit dei punti di modifica del mondo, incluse le mod industriali.

Distinguere concettualmente:

1. Terreno naturale o non osservato.
2. Blocchi piazzati o rimossi manualmente.
3. Modifiche tramite automazione (BuildCraft/RedPower e altre mod, ove osservabili).
4. Alterazioni ambientali o da mob.
5. Provenienza sconosciuta, soprattutto nei vecchi salvataggi.

**L'assenza di un record non prova che un blocco sia naturale.** Un mondo già costruito prima dell'introduzione del sistema avrà dati incompleti.

La provenienza è un indizio, non una definizione: una grotta riadattata può essere un insediamento, e un muro isolato costruito dal giocatore potrebbe non esserlo.

### Implementazione futura [DECISIONE APERTA]

Valutare dati aggregati per regione, rappresentazioni compatte spaziali dei blocchi modificati oppure una combinazione. Evitare registri storici integrali per ogni blocco, costosi e poco utili. Non presumere che gli hook del pack intercettino automaticamente tutti i piazzamenti dei macchinari.

Definire formato persistente, limiti dimensionali, gestione dei chunk, compatibilità con vecchi mondi, rimozioni/sostituzioni, automatismi, fallimenti di rilevazione e politiche di decadimento. Non generare o caricare chunk per calcolare l'attività.

**Protected Interior** e **Settlement Identity** sono concetti distinti: un rifugio può non essere un insediamento e una base può contenere aree esterne non protette. Non confonderli in un unico booleano.

## 10. Tecnologia, difesa e interoperabilità

### Difesa [DIREZIONE APPROVATA]

Fortificazioni e tecnologia devono fornire risposte a minacce concrete. Possibili strumenti [PROPOSTA DA VALIDARE]: sensori, allarmi, trappole alimentate, recinzioni elettrificate, torrette, dispositivi anti-teletrasporto, reti redstone/RedPower e rifornimento automatizzato.

Le difese semplici devono rimanere valide. Una rete IC2/RedPower/BuildCraft può aumentare efficacia e comodità, ma non deve essere l'unica strategia praticabile.

### Interoperabilità [DIREZIONE APPROVATA]

Preferire collegamenti utili tra le mod già presenti a reti tecnologiche parallele. Valutare convertitori IC2 EU/Bluetricity/BuildCraft solo quando producono un vantaggio di gameplay convincente; non imporre un programma generale di conversione energetica.

Per ogni macchina approvata valutare energia, inventari, tubi, redstone, NEI, EMC, automazione, salvataggi e impiego difensivo/esplorativo, **quando pertinenti**. Non è richiesto supportare ogni integrazione per principio.

### Assedi come opportunità [PROPOSTA DA VALIDARE]

Difese avanzate potrebbero consentire di sfruttare controllatamente gli eventi per ottenere ricompense. Evitare farm AFK che eliminino integralmente il pericolo.

## 11. Economia, Equivalent Exchange 2 e scarsità

### Ruolo di EE2 [DIREZIONE APPROVATA]

EE2 deve rimanere potente e soddisfacente, ma non rendere irrilevanti troppo presto miniere, esplorazione, macchinari e risorse eccezionali. Non abolire l'alchimia e non imporre automaticamente un'economia nuova.

### Leve possibili [PROPOSTA DA VALIDARE]

Rivedere EMC di risorse facilmente automatizzabili, evitare loop EMC/EU/materiali, valutare esclusioni selettive o scoperte necessarie prima della replica. Nessuna nuova valuta, sistema di catalizzatori o lunga progressione di trasmutazione va aggiunta senza un preciso bisogno.

### Ender Pearl ed Eye of Ender [DIREZIONE APPROVATA]

**Ender Pearl ed Eye of Ender non devono essere duplicabili o prodotti liberamente attraverso EE2.** La protezione deve considerare anche percorsi indiretti, non soltanto il prezzo EMC esplicito. Verificare l'effettivo comportamento di EE2 1.4.6.5 nel pack e documentare ogni esclusione.

L'economia speciale dell'End deve essere significativa senza moltiplicare materiali intermedi. Evitare di inventare una catena di lavorazione/stabilizzazione enderica come percorso predefinito: **il project owner preferisce la direzione in cui le perle stesse sono ritrovamenti rari**.

### Regola economica

Non introdurre cicli di conversione con rendimento positivo involontario che rendano superflua la progressione. L'automazione ingegnosa è benvenuta; la duplicazione accidentale di risorse di progressione non lo è.

## 12. L'End: perle rare, world flag e scoperta

### Accesso come esplorazione [DIREZIONE APPROVATA]

L'accesso all'End deve essere significativamente più impegnativo del normale farming degli Enderman, senza diventare una catena di lavorazioni industriali obbligatorie.

**Direzione A approvata dal project owner:** le **Ender Pearl** sono risorse rare distribuite fra regioni speciali e fenomeni del mondo. Gli Enderman comuni non devono costituire una fonte ordinaria e illimitata di perle. L'ottenimento passa soprattutto da esplorazione, scoperte e condizioni ambientali/occorrenze eccezionali.

Non scegliere per default la proposta alternativa di «perla instabile da stabilizzare» o una ricetta multi-macchinario: è stata esplicitamente scartata come direzione preferita.

### Disponibilità mondiale e world flag [DIREZIONE APPROVATA COME MODELLO]

Il project owner vuole esplorare un modello con un numero limitato di opportunità e una **riserva totale mondiale Y**, ripartita fra grandi regioni/biomi speciali (in numero ancora da decidere, indicativamente quattro o cinque).

Le **world flag persistenti** rappresentano scoperte, ricompense già ottenute e condizioni concluse. Il gioco non deve esplicitare al giocatore questi flag tramite missioni, contatori o barre boss.

Distinguere:

- Categoria di regione/bioma.
- Istanza concreta di luogo, struttura o fenomeno.
- Opportunità di ottenere perle e sua identità stabile.
- Stato persistente dell'opportunità e vincoli sulla disponibilità mondiale.

Un bioma può comparire più volte; la semplice presenza di una nuova istanza del bioma non deve consentire di eludere il totale mondiale tramite esplorazione infinita.

**Non impostare numeri X/Y, percentuali o una struttura di salvataggio definitiva senza decisione del project owner.** La presenza di una world flag è una direzione di design, non un via libera per implementare un grande framework di quest nascosto.

### Rarità e rischio di mondo impossibile [DECISIONE APERTA]

Il modello di disponibilità mondiale finita è intenzionale. Resta da scegliere come gestire distruzione, lava, morte, caduta nel vuoto, oggetti persi, perdita dei salvataggi, Eye of Ender consumati durante la ricerca e frame del portale.

Valutare con il project owner se la disponibilità sia:

- **Rigidamente finita:** le perle scomparse restano perdute.
- **Finita per scoperte con recupero condizionato:** le opportunità non aumentano, ma alcune perdite possono essere recuperate attraverso regole precise.
- **Finita con eccezioni rare:** piccole fonti ulteriori in condizioni eccezionali.

**Nessuna di queste politiche è approvata come definitiva.** Non introdurre recupero automatico, item indistruttibili, duplicazione nascosta o nuove fonti indefinite per «risolvere» il problema senza una scelta esplicita.

### Eventi narrativi invisibili [DIREZIONE APPROVATA]

Gli avvenimenti che permettono scoperte possono essere programmati in modo rigoroso, senza essere annunciati come quest o miniboss. Possono manifestarsi attraverso comportamento di creature, cambiamenti ambientali, luoghi particolari o eventi rari.

Non introdurre boss bar, UI di missioni o una sequenza Twilight Forest-like per amministrare la progressione.

### Drago [DIREZIONE APPROVATA]

Il combattimento finale deve essere profondamente più interessante del vanilla: fasi o situazioni differenziate, movimento, gestione dello spazio, cristalli/strutture e comportamento tattico. Non basta aumentare HP e danno. La meccanica precisa e le conseguenze narrative della vittoria restano [DECISIONE APERTA].

## 13. Crafting, inventari e qualità della vita

### Filosofia [DIREZIONE APPROVATA]

Le interfacce devono ridurre il lavoro meccanico senza sostituire le scelte creative. Preservare l'estetica e le convenzioni del Minecraft 1.2.5 e compatibilità con NEI/Inventory Tweaks.

Priorità di QoL includono: trasferimenti ricetta quando sensati, indicazione ingredienti mancanti, rifornimento rapido, regole di slot rispettate, auto-refill, sorting, deposito corrispondenti, navigazione NEI e tooltip essenziali, evitando duplicazioni di sistemi modded già presenti.

### Equipaggiamento rapido [PROPOSTA CONCRETA]

**Shift + Click su un pezzo d'armatura nell'inventario** dovrebbe equipaggiarlo direttamente se lo slot compatibile è libero, invece di mandarlo di norma nella hotbar. Se non applicabile, conservare il comportamento atteso del container. La politica di sostituzione dell'armatura già indossata è [DECISIONE APERTA]; non effettuare swap distruttivi o incontrollati.

### Backport e griglie personalizzate

Hopper e altri QoL backport possono avere valore autonomo. **Non trattare la griglia 5×5 come requisito di prodotto**: il crafting avanzato resta [PROTOTIPO SOTTO REVISIONE]. Nuove ricette devono preferire le possibilità esistenti, evitando di rendere necessarie GUI aggiuntive senza beneficio dimostrabile.

### Orientamento senza quest [DIREZIONE APPROVATA]

Tooltip e informazioni essenziali possono spiegare un comando o una macchina; scoperta del mondo ed eventi restano prevalentemente impliciti. Eventuali achievement devono orientare, non prescrivere.

### Vegetarian Locke [PROPOSTA A PRIORITÀ BASSA]

Possibile modalità opzionale basata su alimentazione vegetale e assenza di uccisione di animali, con semi, piante e ricette coerenti con Tekkit. Una piccola macchina alimentare IC2/BuildCraft è giustificabile solo se risolve un problema reale; non introdurre un'industria culinaria generalizzata.

**[DECISIONI APERTE]** modalità volontaria o vincolo applicato dal gioco, regole sugli animali, selezione alla creazione del mondo, compatibilità con salvataggi e ricette. Non anticipare l'implementazione.

## 14. Bilanciamento e progressione della minaccia

1. **Non tutti i giorni sono uguali.** La luna piena rappresenta un culmine; fasi intermedie e mondo ordinario contribuiscono alla minaccia.
2. **No scaling infinito.** Le zone iniziali devono diventare realmente più facili grazie a equipaggiamento e conoscenza.
3. **Difficoltà locale, non ricchezza punita.** La presenza prolungata può essere un segnale; grandezza della fabbrica e età del mondo non devono creare automaticamente orde incontrollabili.
4. **Composizione prima dei numeri.** Mob con ruoli, armature, armi, tattiche e condizioni ambientali interessanti prevalgono sui semplici aumenti di HP/danno.
5. **Limiti rigorosi.** Mob attivi, tentativi, scansioni, tempi ed effetti sul mondo devono essere limitati e osservabili.
6. **Preparazione premiata.** Torce, costruzioni, equipaggiamento e macchine offrono vantaggi misurabili, senza garantire invulnerabilità universale.
7. **Griefing controllato.** Le perdite strutturali non devono annullare arbitrariamente ore di lavoro.
8. **Esplorazione significativa.** Le risorse rare non devono diventare banali attraverso EMC o farm ripetitive.
9. **Difficoltà leggibile.** Per il giocatore cause e contromisure devono essere intuibili; per lo sviluppatore, diagnostiche e test devono essere espliciti.
10. **Tensione e respiro.** Anche senza salto della notte, devono esserci periodi in cui costruire, pianificare e recuperare in tranquillità.

## 15. Architettura e vincoli per agenti

### Confini di responsabilità [DIREZIONE APPROVATA]

- **LunarService:** fasi, transizioni e finestre temporali; non conosce insediamenti, mob o loot.
- **Encounter/SiegeDirector:** sceglie e gestisce incontri; PLAYER_ASSAULT, pattuglie e futuro SITE_SIEGE hanno identità distinte.
- **Spawn/AI:** gestisce posizione, validazione, popolazione e comportamento, senza ricalcolare autonomamente tutta la minaccia.
- **Territorial Activity / Threat Assessment:** future autorità per segnali locali e intensità, non sistemi UI o calendario duplicato.
- **Settlement Recognition / Block Provenance:** futuri strumenti per riconoscere spazi e insediamenti, senza dichiarare obbligatori letti, porte o marker.
- **Economia, EMC ed energia:** verificano conversioni e scarsità; non impongono catene di nuove macchine senza ragione.
- **UI e Dev Bridge:** osservano e presentano lo stato; non diventano autorità di gameplay.

Non costruire API speculative o infrastrutture generiche prima di aver validato almeno un consumatore reale.

### Compatibilità

- Verificare sempre le vere API di Minecraft 1.2.5 / Tekkit Classic 3.1.2, non assumere meccaniche moderne.
- Mantenere la distinzione fra profilo di compilazione MCP/Forge e runtime del pack installato.
- Proteggere blocchi, Item ID, TileEntity, NBT e salvataggi preesistenti; pianificare migrazioni idempotenti.
- Evitare sostituzioni invasive di classi vanilla/modded e conflitti con NEI/Inventory Tweaks.
- Non effettuare scansioni costose, chunk load forzati o work per tick non limitato.
- Il gameplay singleplayer può essere la prima implementazione; il multiplayer futuro richiederà autorità server e sincronizzazione appropriate, senza pretendere supporto già verificato.
- Distinguere prove portabili, compilazione, runtime Tekkit Dev e playtest umano. Non presentare un test automatico come prova di bilanciamento soggettivo.

## 16. Roadmap strategica e release

**[DIREZIONE APPROVATA]** La linea `0.1.0-alpha.N` può contenere diverse feature e prototipi di gameplay. Passare a `0.2`, `0.3` e oltre quando si affrontano grandi architetture o sistemi, non soltanto per aver aggiunto un nuovo mob.

| Linea | Tema strategico |
| --- | --- |
| **0.1 — Survival Foundations** | Calendario, PLAYER_ASSAULT, pattuglie, equipaggiamento dei mob, primi ruoli tattici, revisione ragionata di luce/spawn, prototipo sonno e prime difese. |
| **0.2 — Territorial Activity & Threat** | Segnali territoriali, heatmap limitate, valutazione locale della minaccia e intensità/composizione degli incontri. |
| **0.3 — Settlement Recognition** | Riconoscimento di insediamenti e avamposti, valutando provenienza delle modifiche, attività e contesto edilizio. |
| **0.4 — SITE_SIEGE & Fortress Warfare** | Assedi reali a siti persistenti, obiettivi tattici, difese e griefing controllato. |
| **0.5 — Dangerous Exploration** | Prime grandi regioni ostili, possibili aree sottomarine ed enderiche, preparazione e scoperte. |
| **0.6 — Industrial Integration** | Revisione selettiva di macchine, EMC e interoperabilità, soltanto dove supportano il gioco. |
| **0.7 — The End Awakens** | Fenomeni enderici, distribuzione delle perle, world flag e percorso narrativo ambientale verso l'End. |
| **0.8 — Endgame & Dragon** | Accesso all'End, drago rielaborato, sfide e ricompense finali. |
| **0.9 — Integration & Balance** | Bilanciamento trasversale, compatibilità, prestazioni, progressione e rifinitura. |
| **1.0 — Complete Overhaul** | Prima esperienza survival-industriale coerente e consolidata dall'inizio all'End. |

**La roadmap indica il sistema protagonista di ogni linea, non vieta prototipi anticipati o manutenzione trasversale.** Non trasformare questa tabella in impegni di consegna o feature già implementate. Rimane applicabile la politica di versioning in `docs/agents/VERSIONING_POLICY.md`.

## 17. Decisioni aperte — Registro esplicito

Gli agenti non devono risolvere unilateralmente queste questioni; possono utilizzare default locali conservativi per un prototipo, documentandoli come tali.

### End e risorse

- Numero e identità delle grandi regioni speciali; eventuali biomi alternativi.
- Numero **X** di Ender Pearl associato a ciascuna regione e limite totale **Y** per mondo.
- Metodo per associare world flag a istanze di luoghi, scoperte o fenomeni e per impedire duplicazioni o bypass tramite nuove generazioni di biome.
- Quantità di riserva necessaria per Eye of Ender, ricerca dello stronghold, perdita di occhi e completamento del portale.
- Perdite irreversibili, recupero condizionato o rarissime eccezioni alla riserva finita.
- Rimozione dei drop ordinari degli Enderman: eccezioni, compatibilità con vecchi mondi e alternative di ottenimento.
- Politica esatta di esclusione EE2 e audit dei percorsi di conversione indiretti.
- Regole e contenuti degli eventi enderici nascosti; drago e conseguenze della vittoria.

### Sopravvivenza e insediamenti

- Parametri reali della pressione lunare e comportamento nelle notti intermedie.
- Regole precise di luce e spawning in spazi aperti, grotte, rifugi e regioni speciali.
- Metodo tecnico per distinguere un interno protetto da una grotta o da una costruzione incompleta.
- Interazione fra provenienza dei blocchi, attività territoriale, forma degli spazi e riconoscimento di una base.
- Granularità e persistenza del tracciamento delle modifiche, comprese quelle effettuate da automazione modded.
- Formula della minaccia locale, decadimento, saturazione, limiti e comportamento nei mondi esistenti.
- Semantica definitiva del divieto di salto notturno, opzioni per mondo e futura compatibilità multiplayer.
- Entità e gravità dei danni consentiti alle costruzioni durante SITE_SIEGE.

### Tecnologia, contenuti e QoL

- Permanenza, dismissione o rimozione definitiva di distillazione frazionata, crafting 5×5 e altri prototipi industriali.
- Quali nuove difese tecnologiche siano effettivamente necessarie e a quali minacce debbano rispondere.
- Dettagli della regione sottomarina: ossigeno, ricarica, armature e generazione del mondo.
- Dettagli della regione enderica e ruolo degli Enderman specifici.
- Eventuali nuove conversioni energetiche e loro economie, senza obbligo di introdurle.
- Politica di Shift + Click per armatura già indossata e interferenze con container modded.
- Vegetarian Locke: attivazione, vincoli e minima quantità di nuove colture/cibo.

## 18. Istruzioni permanenti per gli agenti LLM

Prima di proporre o implementare una feature:

1. Leggi **integralmente** questo documento, `AGENTS.md` e le specifiche pertinenti.
2. Verifica cosa esiste davvero nel branch corrente: **una direzione di design non è un'implementazione**.
3. Classifica l'intervento: [DIREZIONE APPROVATA], [PROPOSTA DA VALIDARE], [DECISIONE APERTA] oppure [PROTOTIPO SOTTO REVISIONE].
4. Non convertire automaticamente una proposta in requisito: chiedi una decisione quando la scelta è strutturale o irreversibile.
5. Preferisci modificare e integrare meccaniche già presenti a creare nuove risorse, macchine, GUI o economie.
6. Evita quest obbligatorie, grind wall, sistemi di progressione espliciti non richiesti e catene di crafting artificiali.
7. Mantieni distinti PLAYER_ASSAULT, SITE_SIEGE, calendario, minaccia territoriale, provenienza blocchi e identità degli insediamenti.
8. Preserva varietà delle soluzioni, potere effettivo dell'equipaggiamento avanzato e momenti di calma.
9. Verifica il vero runtime legacy e proteggi la compatibilità dei salvataggi; non cancellare una feature prototipale senza valutare le conseguenze.
10. Mantieni compatti commit e PR, test mirati e prove riproducibili. Non presentare supposizioni come risultati osservati.
11. Nel report spiega il valore di gameplay, le integrazioni, i rischi, le decisioni lasciate aperte e le funzionalità deliberatamente escluse.
12. **Non fare lavoro perché la feature è già stata iniziata: fallo perché serve all'esperienza di gioco.**

## 19. Criteri per accettare una nuova feature

Una proposta deve superare queste domande:

1. Quale problema di gameplay, esplorazione, costruzione o atmosfera risolve?
2. Può essere risolto integrando meglio una meccanica Tekkit già presente?
3. Introduce una possibilità reale o soltanto una nuova ricetta/barriera?
4. Il giocatore può scegliere più di una risposta ragionevole?
5. La scoperta rimane interessante senza una quest esplicita?
6. Conserva il senso di potenza del progresso tecnologico?
7. Rispetta costruzioni, salvataggi e tempo investito dal giocatore?
8. La difficoltà resta leggibile e proporzionata?
9. I sistemi di EE2, inventari, energia e automazione potrebbero aggirarla o duplicarla?
10. Ha un perimetro testabile e può essere rimossa senza danni se il prototipo fallisce?

**Regola di veto:** se una soluzione aumenta soprattutto la complessità e non amplia in misura convincente possibilità, tensione, atmosfera o scoperta, va semplificata oppure scartata.

---

## Sintesi in una frase

> **Una Tekkit Classic fedele alla propria libertà, dove il mondo è abbastanza vivo e ostile da dare significato alla tecnologia e alle fortificazioni, l'esplorazione custodisce risorse e misteri davvero preziosi, e l'End diventa il culmine di una storia che il giocatore ricostruisce senza che il gioco gliela racconti — senza quest obbligatorie, grinding artificiale o un accumulo indiscriminato di nuovi sistemi.**
