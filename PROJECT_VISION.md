# ZedsTekkitOverhaul — Project Vision & Game Design Charter

> **Documento guida permanente per sviluppatori e agenti LLM.**
> 
> **Target:** Minecraft Java Edition 1.2.5, ecosistema Tekkit Classic.
> **Scopo:** mantenere una visione coerente durante lo sviluppo di singole feature. Questo documento descrive **perché** e **come** il progetto deve evolvere; non sostituisce le specifiche tecniche delle singole funzionalità.
> **Stato:** visione di prodotto. I requisiti marcati **[DIREZIONE]** sono intenti progettuali consolidati; **[PROPOSTA]** sono soluzioni da prototipare e validare; **[DA DECIDERE]** sono questioni deliberatamente aperte. Le feature menzionate non sono automaticamente da considerare implementate.

---

## 1. Manifesto

**ZedsTekkitOverhaul vuole essere la Tekkit classica che ricordiamo, ma inserita in un Minecraft più vivo, ostile, misterioso e degno della potenza che il giocatore può raggiungere.**

Il cuore del progetto non è la complessità tecnologica. È la libertà di costruire, esplorare, sperimentare, difendersi e diventare straordinariamente potenti. La tecnologia, l'alchimia e l'automazione sono **strumenti**, non il fine ultimo. Una piccola farm intelligente deve essere una strada legittima; una grande fabbrica deve essere un'opportunità, mai un pedaggio obbligatorio.

Minecraft vanilla offre una sandbox eccezionale ma, soprattutto in Tekkit 1.2.5, il mondo finisce per opporre poca resistenza alla crescita del giocatore. IC2, RedPower, BuildCraft ed EE2 ampliano enormemente le possibilità produttive; la pressione esercitata dai nemici e dall'ambiente non cresce in misura comparabile. Il risultato è che parte del grinding porta a oggetti potentissimi senza una ragione concreta per usarli.

**Il progetto deve correggere questa asimmetria senza sacrificare lo spirito del Minecraft di quell'epoca: “fai un po' come vuoi”.**

### La promessa al giocatore

- Puoi iniziare senza conoscere a fondo le mod tecniche. Il gioco ti suggerisce possibilità, non ti impone una sequenza di quest.
- Una casa improvvisata può bastare all'inizio; col tempo avrai veri motivi per costruire e migliorare una **fortezza**.
- Il mondo contiene zone e minacce davanti alle quali è sensato ritirarsi, prepararsi e tornare.
- Puoi affrontare i problemi con combattimento, costruzione, equipaggiamento, ingegno, alchimia o automazione, spesso combinandoli.
- I macchinari devono renderti capace di fare cose nuove o di farle meglio, non obbligarti a eseguire centinaia di microcrafting.
- La progressione avanzata deve sentirsi potente. Red Matter Armor, QuantumSuit e sistemi produttivi evoluti devono avere un peso reale.
- Sotto la superficie del mondo esiste un mistero progressivamente riconducibile all'**End**, agli Enderman e al drago, raccontato soprattutto tramite gameplay e ambiente.

## 2. Identità: cosa siamo e cosa non siamo

### Siamo

- **Una sandbox survival-industriale**, con minacce crescenti e molte soluzioni valide.
- **Una reinterpretazione rispettosa di Tekkit Classic**, non un totale cambio di genere.
- **Un overhaul integrato**, in cui le meccaniche delle varie mod si incontrano e si potenziano a vicenda.
- **Un'esperienza technical-light-friendly**: accessibile a chi ama creare qualche farm RedPower/EE2 e giocare con IC2, ma con profondità per chi desidera costruire impianti maggiori.
- **Un Minecraft atmosferico e sistemico**: il mondo reagisce, il giocatore comprende i segnali e prende decisioni.

### Non siamo

- Un quest pack con percorso obbligatorio di crafting.
- Un GregTech più semplice: non vogliamo che la tecnologia diventi il contenuto principale e autosufficiente.
- Un modpack *skyblock*, *stoneblock*, *oceanblock* o un'esperienza artificialmente confinata per forzare la progressione.
- Un kitchen sink che accumula contenuti scollegati.
- Un survival basato soltanto su mob con HP e danni moltiplicati.
- Un gioco che punisce continuamente chi vuole costruire con calma.
- Un sistema dove la difficoltà cresce indefinitamente in proporzione al tempo trascorso o alla ricchezza posseduta.

**Test di identità:** se una feature aggiunge soltanto una nuova ricetta costosa, una nuova valuta, una nuova schermata o un requisito artificiale senza ampliare le possibilità del giocatore, probabilmente non appartiene al progetto.

## 3. Feeling e atmosfera

### Familiarità prima della novità

Il primo impatto deve ricordare Minecraft 1.2.5 e Tekkit Classic: estetica riconoscibile, interfacce sobrie, esplorazione libera, scoperta casuale e costruzioni create con blocchi concreti. Le aggiunte moderne devono sembrare **evoluzioni naturali**, non l'importazione di un altro modpack.

### Tensione intermittente, non stress continuo

Il mondo deve alternare **calma, esplorazione, preparazione, presagi, assedio e ripresa**. La tensione funziona se il giocatore ha tempo per anticiparla e comprenderla. Gli eventi più violenti devono essere telegraphed, con segnali leggibili e contromisure praticabili. La notte ordinaria non deve diventare sempre una luna piena.

### Mistero dell'End

Gli Enderman sono il volto riconoscibile di una minaccia anomala: osservano, appaiono in luoghi inattesi, spostano blocchi, manipolano il territorio, talvolta si coordinano. L'inquietudine nasce da **comportamenti e conseguenze** più che da jumpscare, lore esplicita o statistiche enormi.

L'End deve mantenere la sua estraneità e ambiguità. Non è necessario spiegare tutto, né stabilire immediatamente se gli Enderman siano semplicemente malvagi. Il drago e la dimensione dell'End costituiscono il punto di convergenza del percorso, con un omaggio al finale originale di Minecraft.

### Narrazione ambientale

Preferire: strutture anomale, regioni alterate, rovine, segnali nella natura, incontri rari, comportamenti dei mob, effetti sulle basi, indizi che il giocatore collega da sé.

Evitare: lunghi libri obbligatori, dialoghi espositivi, NPC che assegnano missioni sequenziali, lore che interrompe il gioco o spiega ogni mistero.

## 4. I pilastri di game design

### 4.1 Libertà di approccio [DIREZIONE]

La progressione è **aperta**, ma il mondo non è privo di pericoli. È legittimo che una regione o il boss finale siano praticamente proibitivi senza preparazione; non è legittimo imporre una sola macchina, un'unica armatura o una catena di crafting arbitraria come risposta.

Per ogni ostacolo importante cercare più soluzioni: equipaggiamento, strategia, ingegneria ambientale, difesa, automazione, alleanze fra sistemi. Alcune capacità possono essere eccezionalmente utili senza diventare formalmente obbligatorie.

### 4.2 Tecnologia come mezzo [DIREZIONE]

Una macchina deve rispondere a un bisogno del mondo: produzione, difesa, mobilità, approvvigionamento, controllo del territorio, esplorazione, sicurezza, gestione dell'energia. Sviluppare capacità tecniche dà libertà; non deve somigliare a un esame di ingegneria.

### 4.3 Fortezza del giocatore [DIREZIONE]

**La base fortificata è un elemento centrale e riconoscibile del loop di gioco.** Non prescriverne la forma: possono esistere un castello, un bunker, una fabbrica cinta da mura, una base sotterranea o un avamposto automatizzato.

Le scelte edilizie devono avere conseguenze funzionali: accessi, barriere, illuminazione, percorsi, fossati, punti di fuoco, ridondanza energetica, aree vulnerabili. Il giocatore deve desiderare di migliorare la fortezza dopo un assedio, non essere costretto a ricostruire integralmente ciò che ha perso.

### 4.4 Difficoltà significativa, non attrito [DIREZIONE]

La difficoltà giusta nasce da decisioni, rischi, tattica, conoscenza, risorse, progettazione e adattamento. Non da 200 click ripetuti, 864 componenti intermedi o continue operazioni di svuotamento degli inventari.

**Eliminare i microcrafting non significa eliminare la necessità di pianificare una produzione.**

### 4.5 Progressione di potenza percepibile [DIREZIONE]

Un giocatore con armatura avanzata deve dominare minacce che prima erano pericolose. Evitare scaling globale che annulli ogni conquista. Le minacce di late game possono cambiare *tipo* di sfida — teletrasporto, sabotaggio, controllo del territorio, attacchi coordinati — senza rendere ogni zombie un boss.

## 5. Loop di gioco desiderato

**Esplora → scopri pericoli e risorse → torna e prepara la base → migliora strumenti/produzione/difese → affronta eventi → ottieni nuove possibilità → esplora più lontano.**

L'alternanza non deve essere forzata da quest. È il mondo a rendere desiderabile il passaggio da una fase all'altra.

### Progressione indicativa, non sequenza obbligatoria

- **Inizio:** sopravvivenza familiare, rifugio, primi esperimenti tecnici, anomalie rare.
- **Early-mid:** prime farm e macchine, miglioramento della base, regioni che richiedono preparazione, incursioni contenute.
- **Mid:** automazione più utile, difese efficaci, esplorazione di zone ostili, crescente attività dell'End.
- **Late:** QuantumSuit, Red Matter e altre soluzioni potenti, grandi capacità produttive e difensive, preparazione alle minacce maggiori.
- **Endgame:** accesso all'End più impegnativo, drago profondamente rielaborato, confronto finale che premi equipaggiamento e strategia.

Queste etichette non devono trasformarsi in *gates* rigidi.

## 6. Minacce, biomi e mob

### Biomi pericolosi [DIREZIONE]

Alcune regioni devono scoraggiare l'ingresso prematuro attraverso nemici, ambiente, visibilità, stato del territorio o altri rischi. Il giocatore deve poter tentare, comprendere perché è in difficoltà, ritirarsi e tornare preparato.

**[PROPOSTA]** Zone glaciali con esigenze termiche, paludi contaminate, regioni segnate dall'End, aree con mob specializzati. Non sono definizioni definitive né un obbligo di introdurre sistemi survival complessi come fame/sete aggiuntive.

**Regola:** evitare i muri invisibili basati su level gating. Preferire ostacoli diegetici e aggirabili creativamente.

### Enderman e varianti [DIREZIONE + PROPOSTE]

Ampliare il ruolo degli Enderman: furto di blocchi più significativo, apparizioni inquietanti, stalker occasionali, capacità di interferire con strutture e attacchi coordinati.

Possibili archetipi **[PROPOSTA]**: osservatore/stalker, razziatore, cacciatore, sabotatore, esemplare corrotto, araldo o coordinatore. Non trasformare ogni variante in un semplice aumento di HP.

### Nemici per ruolo durante gli assedi [PROPOSTA]

- Fanteria per pressione numerica.
- Assaltatori per porte e strutture vulnerabili.
- Scalatori o aggiratori per rendere i muri semplici non infallibili.
- Sabotatori/Enderman contro difese periferiche.
- Supporti e nemici d'élite come priorità tattiche.

Ogni nuova capacità nemica dovrebbe suggerire una contromisura leggibile e una nuova possibilità costruttiva.

### Griefing controllato [DIREZIONE]

Gli Enderman e gli assalitori possono rappresentare una minaccia concreta alle difese. Non devono distruggere indiscriminatamente macchinari costosi o cancellare ore di lavoro senza preavviso. Distinguere fra danno recuperabile, perdita significativa e distruzione irreversibile.

Le strategie di fortificazione e automazione devono essere premiate; nessuna difesa dovrebbe essere perfetta in ogni circostanza.

## 7. Calendario lunare e assedi

### Sistema lunare [DIREZIONE]

La luna è il **segnale naturale** attorno al quale organizzare il ritmo di eventi e assedi. Minecraft 1.2.5 ha già fasi lunari: riutilizzare il più possibile la base vanilla e dotarla di servizi interni affidabili.

**[PROPOSTA DI BASELINE]** Otto fasi in otto giorni Minecraft (circa 2 h 40 min a 20 TPS); nuovi mondi iniziano in luna nuova; luna piena come notte del principale evento ciclico. Il bilanciamento finale della frequenza resta aperto: non trattare la durata come dogma.

### Assedi [DIREZIONE]

Durante determinate notti devono presentarsi ondate ostili, inizialmente moderate e più avanti articolate. La luna piena può ospitare l'assedio principale; fasi intermedie possono contenere pattuglie, incursioni leggere e presagi. Gli assedi devono incentivare pianificazione, difesa della base e sperimentazione tecnologica.

**Importante:** il calendario definisce *quando* una finestra è possibile; un sistema separato deciderà *se*, *dove*, *quanto* e *con quali mob* si verifica un evento.

La difficoltà non deve aumentare all'infinito soltanto perché il giocatore costruisce lentamente. Valutare un modello che unisca cicli lunari, milestone significative e attività locale dell'End, con soglie e limiti. Non punire automaticamente la ricchezza o la dimensione della fabbrica.

### Sonno, assenza, fuga [DA DECIDERE]

Definire in modo esplicito la semantica delle notti saltate, delle basi abbandonate, del giocatore in esplorazione e dei server multiplayer. Un assedio non deve essere vanificato banalmente dal sonno né imprigionare il giocatore in un unico playstyle. Evitare punizioni offline non concordate.

### Architettura prevista [DIREZIONE]

Separare almeno:

1. **LunarService**: fasi e calendario.
2. **LunarEvent API**: transizioni, notti, finestre temporali.
3. **Threat/Progression System**: stato della minaccia, in futuro.
4. **SiegeDirector**: selezione e gestione degli eventi, in futuro.
5. **SpawnDirector**: composizione, spawn e comportamenti, in futuro.
6. **Base/Defense Integration**: regole per difese e infrastrutture, in futuro.

Il servizio lunare non deve conoscere tipi di mob, posizioni delle basi o formule di bilanciamento delle orde. Le API devono essere stabili, ma evitare di inventare infrastrutture generiche inutilmente complesse.

## 8. Il ruolo della tecnologia nella difesa

La fortezza deve essere un motivo per utilizzare IC2, RedPower e BuildCraft **insieme**, senza richiedere mega-fabbriche.

**[PROPOSTE]** Torrette o trappole alimentate, recinzioni elettrificate, sensori, allarmi, illuminazione difensiva, dispositivi anti-teletrasporto, reti Redstone/RedPower, approvvigionamento automatizzato di risorse e componenti.

Le soluzioni manuali e semplici devono restare possibili. Un impianto industriale avanzato offre comodità, sicurezza, scala e nuove strategie, non l'unico modo per sopravvivere.

### Assedi come possibile opportunità [PROPOSTA]

I mob speciali potrebbero rilasciare materiali utili. Un giocatore avanzato potrebbe costruire difese che rendono l'assedio non solo sopravvivibile ma, in modo controllato, **sfruttabile**. Fare attenzione a farm AFK ed exploit che annullino completamente il rischio.

## 9. Economia ed Equivalent Exchange 2

### Problema da correggere [DIREZIONE]

La conversione universale tramite EMC, soprattutto se alimentata da farm di cobblestone o altre risorse banali, rischia di cancellare troppo presto il valore di miniere, esplorazione, produzione industriale e risorse rare. La Transmutation Table può diventare una scorciatoia che elimina interi sistemi di gameplay.

### Obiettivo [DIREZIONE]

**EE2 deve essere potente, divertente e progressivamente liberatoria, ma non deve rendere irrilevante il mondo già nell'early o early-mid game.** Non eliminarne la fantasia alchemica; regolarne l'accesso e l'ampiezza.

### Leve di bilanciamento [PROPOSTE, DA VALUTARE]

- Valori EMC rivisti per risorse facilmente automatizzabili.
- Esclusioni o limitazioni EMC per componenti complessi, prodotti industriali e materiali narrativamente speciali.
- Necessità di aver scoperto fisicamente un materiale prima di replicarlo; verificare e, se opportuno, estendere il meccanismo di apprendimento già presente in EE2.
- Sblocco progressivo della capacità di trasmutazione per categorie o materiali specifici.
- Eventuali catalizzatori per certe trasformazioni, **senza** creare una lunga catena di crafting obbligatoria.
- Analisi delle interazioni EMC ↔ EU ↔ altri sistemi e relativi cicli di rendimento.

**Non assumere che tutte queste misure verranno applicate simultaneamente.** Prototipare prima la soluzione meno invasiva che mantenga la miniera e l'esplorazione rilevanti.

### Regola economica

Gli scambi e le catene di crafting/conversione non devono generare risorse o energia dal nulla attraverso cicli involontari a rendimento netto positivo, quando questo rende inutile il resto della progressione. L'automazione ingegnosa è desiderata; i loop infiniti accidentali vanno corretti.

Distinguere fra risorsa naturale, ingrediente industriale, tecnologia costruita e materiale speciale. L'EMC può ridurre il bisogno di raccogliere ripetutamente qualcosa; non deve sostituire automaticamente ogni scoperta o lavorazione.

## 10. Integrazione fra mod ed energia

### Interoperabilità [DIREZIONE]

Le mod già presenti devono dialogare. Il progetto non deve aggiungere sistemi paralleli chiusi quando può creare collegamenti significativi.

Caso centrale: energia elettrica IC2 (EU) e sistema energetico RedPower 2 (Bluetricity). **[PROPOSTA]** Utilizzare convertitori o adapter dedicati anziché riscrivere il modello energetico interno di ogni mod. Preservare caratteristiche e identità dei sistemi.

Valutare anche interazioni con BuildCraft ed EE2. Conversioni e macchinari devono dichiarare chiaramente unità, capacità, costi e rendimento. Bloccare o correggere oggetti/ricette del pack originale soltanto quando necessario per evitare exploit, bypass gravi o contraddizioni sistemiche.

### Integrazione come comportamento predefinito

Ogni nuovo macchinario deve essere esaminato rispetto a:

- Energia: sorgenti, convertitori, limiti, rendimento.
- Oggetti: slot, tubi, Hopper, modalità di input/output.
- Ricette: consultazione in NEI e scoperte alternative.
- Automazione: segnali Redstone/RedPower, logistica BuildCraft.
- Economia: EMC, materiali duplicabili, possibili loop.
- Difesa: eventuali applicazioni pratiche nella fortezza.

L'integrazione non è obbligatoriamente con **tutti** i sistemi, ma le decisioni devono essere consapevoli e documentate.

## 11. Crafting e Quality of Life

### Filosofia [DIREZIONE]

**Le interfacce devono ridurre il lavoro meccanico, non prendere decisioni creative al posto del giocatore.** Non importare ciecamente interfacce moderne; rispettare il linguaggio visivo della 1.2.5.

Riutilizzare e ampliare NEI e Inventory Tweaks già presenti. Gli agenti hanno a disposizione il codice decompilato nel workspace e devono verificarne i comportamenti prima di aggiungere funzioni duplicate.

### Priorità funzionali [DIREZIONE]

- Trasferimento ricette tipo pulsante `+` JEI: ingredienti posseduti disposti automaticamente nella crafting grid compatibile.
- Evidenza degli ingredienti mancanti, quantità disponibili e alternative valide.
- Ripetizione del crafting e riempimento bilanciato per ridurre operazioni manuali.
- Supporto pieno alle griglie personalizzate, inclusa la **5×5** del progetto.
- Restituzione rapida degli ingredienti dalla griglia.
- Navigazione chiara NEI fra ricette, utilizzi, alternative e lavorazioni industriali.
- Input/output manuale rapido nei macchinari, con regole di slot rispettate.
- Deposito corrispondenti (*Deposit Matching*), deposito/prelievo rapidi, slot protetti e consolidamento stack.
- Auto-refill, sorting e scorciatoie coerenti con Inventory Tweaks, senza duplicarle.
- Tooltip brevi sui macchinari: alimentazione, input/output, stato, ragioni dell'inattività.
- Keybind configurabili e pulsanti discreti che non coprano GUI legacy.

### Opzioni da valutare [PROPOSTA]

Preferiti e cronologia NEI, liste materiali, quantità craftabili, ricerca nei container, crafting da chest adiacenti, calcolo ricorsivo degli ingredienti.

Le ultime due richiedono particolare cautela: non devono cancellare il valore della logistica, dell'organizzazione degli inventari e dell'automazione industriale.

### Backport vanilla [DIREZIONE]

Introdurre gli **Hopper** moderni nella 1.2.5, con comportamento comprensibile e integrazione nei container del pack. Devono convivere con BuildCraft Pipes e RedPower Tubes: Hopper per automazione semplice e locale, sistemi delle mod per trasporto e logistica più elaborata. Valutare altri backport/QoL caso per caso, non come checklist di tutte le feature moderne.

### Indicazioni in gioco [DIREZIONE]

Achievement/advancement come sistema leggero di **orientamento e scoperta**, non quest obbligatorie. Crafting visibili, tooltip chiari, macchinari autoesplicativi per quanto possibile. Niente libri-manuale lunghissimi né percorsi lineari prescritti.

## 12. L'End e il finale

### Il finale come omaggio [DIREZIONE]

L'End originale di Minecraft è il riferimento tematico centrale. La presenza degli Enderman nell'Overworld deve rendere la dimensione finale una scoperta preparata da ore di eventi e anomalie.

L'accesso all'End deve diventare più impegnativo di una semplice ripetizione del procedimento vanilla, ma evitare di richiedere una fabbrica industriale monumentale o ricette spropositate. **L'End deve richiedere preparazione e versatilità, non lavoro amministrativo.**

### Proposte di progressione [PROPOSTA]

- Indizi distribuiti nel mondo e nelle Stronghold.
- Regioni o strutture pericolose che contengono conoscenze/materiali utili.
- Possibili interazioni tecnologiche o alchemiche nella stabilizzazione/attivazione del portale.
- Diverse soluzioni plausibili, non un unico percorso di macchinari imposto.

### Drago [DIREZIONE]

La battaglia deve essere significativamente più impegnativa e spettacolare: fasi differenziate, pressione di altri nemici, interazione con cristalli/strutture, necessità di movimento e strategia. **Non limitarsi ad aumentare HP e danni.** Gli oggetti di tier alto devono essere utili senza rendere automaticamente banale l'intero incontro.

La natura esatta del drago, le meccaniche delle fasi e le conseguenze della vittoria restano **[DA DECIDERE]**.

## 13. Progressione e difficoltà: regole di bilanciamento

1. **Niente scaling infinito dei mob comuni.** Le zone iniziali devono diventare realmente più facili dopo una crescita significativa.
2. **Niente punizioni automatiche per la creatività.** Una fabbrica grande non deve attirare assedi ingestibili solo perché è grande.
3. **Difese efficaci, non invincibili.** Ogni attacco speciale deve poter essere previsto o contrastato in modo comprensibile.
4. **Perdita proporzionata.** Una notte difficile può consumare risorse o danneggiare difese; non dovrebbe distruggere arbitrariamente ore di lavoro.
5. **Esplorazione sempre utile.** Limitare la capacità dell'EMC di sostituire subito ogni risorsa rara o speciale.
6. **Tecnologia non obbligatoriamente massimizzata.** Un technical player leggero deve poter affrontare il cuore del gioco con una base ben pensata e una selezione ragionevole di macchine.
7. **Sfide multidimensionali.** Più tattiche e interazioni sistemiche, meno semplici moltiplicatori numerici.
8. **Nessuna grind wall artificiale.** Costi e ricette devono avere una giustificazione di gameplay.
9. **Feedback leggibile.** Il giocatore deve capire almeno in modo approssimativo cosa lo ha sconfitto e cosa potrebbe fare diversamente.
10. **Ricompensa della preparazione.** Una base e un equipaggiamento migliori devono produrre benefici tangibili.

## 14. Linee guida architetturali trasversali per gli agenti

### 14.1 Non costruire scatole chiuse

Ogni feature va sviluppata con il suo contesto di integrazione, anche quando non si implementano ancora i collegamenti futuri. Separare la logica centrale dai consumatori e fornire API/interfacce appropriate, senza sovra-ingegnerizzare.

Prima di implementare una feature, chiedersi:

- Quali sistemi già presenti la utilizzano o la influenzano?
- Quali sistemi futuri potrebbero ascoltare i suoi eventi o interrogare il suo stato?
- Quali regole di compatibilità, automazione o economia vengono toccate?
- Come può essere testata da sola e in integrazione?
- Quale comportamento resta valido se il modulo futuro non esiste ancora?

### 14.2 Confini consigliati

- **Calendario lunare** informa dei tempi, non genera mob.
- **Direttore degli assedi** decide eventi e obiettivi, non reimplementa il calendario.
- **Sistema spawn/AI** esegue regole di spawning e comportamento, non calcola da sé la progressione globale.
- **Sistema minaccia** aggrega segnali di progressione e attività locale, non possiede le GUI.
- **Economia/EMC/energia** gestisce conversioni e vincoli, non hardcoda le difese.
- **Integrazione inventari/ricette** riutilizza container e recipe handler delle mod, senza duplicare i motori di crafting.
- **Interfacce** presentano stato e comandi, ma non sono autorità di gameplay.

### 14.3 Compatibilità e qualità

- Verificare sempre le API reali e il codice **Minecraft 1.2.5**, evitando assunzioni prese da versioni moderne.
- Rispettare salvataggi preesistenti e prevedere migrazioni idempotenti.
- In multiplayer, il server è autorevole per spawn, risorse, inventari, eventi e progressione.
- Nessuna duplicazione di oggetti né cicli di energia/EMC involontariamente positivi.
- Preferire adapter, eventi e integrazioni ai fork invasivi delle mod legacy, salvo necessità motivata.
- Non introdurre polling costoso, scansioni indiscriminate dei chunk o ricalcoli per tick senza necessità.
- Ogni nuova GUI deve convivere con NEI, Inventory Tweaks e i container del pack.
- Implementare feature piccole con test di regressione, non refactor massivi non richiesti.

## 15. Come decidere se una nuova feature è coerente

Per ogni proposta, l'agente o lo sviluppatore deve poter rispondere a queste domande:

1. **Quale problema di gioco risolve?** Non basta che sia tecnicamente interessante.
2. **Quale possibilità nuova introduce?** Non deve essere solo un nuovo requisito.
3. **Quale libertà conserva?** Può il giocatore scegliere un'altra strada?
4. **Quale attrito elimina o aggiunge?** I click ripetuti non sono profondità.
5. **Come interagisce con le mod esistenti?** Energia, oggetti, automazione, crafting, EMC.
6. **Come interagisce con il loop di fortezza/esplorazione/minaccia?** Anche un legame indiretto può essere valido.
7. **Quali exploit apre?** Soprattutto conversioni cicliche, transfer inventory e farm AFK.
8. **Come comunica le sue regole?** Preferire affordance e feedback in gioco a manuali obbligatori.
9. **È coerente col feeling legacy?** Interfaccia, ritmo, estetica, libertà.
10. **È testabile e reversibile?** Favorire sviluppo incrementale e compatibilità con i mondi.

**Regola di veto:** se una soluzione riduce la libertà senza produrre un miglioramento significativo in atmosfera, profondità o leggibilità, deve essere riprogettata.

## 16. Roadmap tematica, non lista di impegni già completati

### Fondazioni e interfacce

- [ ] Audit delle funzionalità esistenti e del loro stato nel repository.
- [ ] QoL NEI/Inventory Tweaks e crafting 5×5.
- [ ] Hopper vanilla e interoperabilità con inventari modded.
- [ ] Informazioni in gioco essenziali e achievement non prescrittivi.

### Sistemi del mondo

- [ ] Calendario lunare persistente e visibile.
- [ ] API di eventi temporali e finestre per futuri assedi.
- [ ] Progressione della minaccia separata dal calendario.
- [ ] Assedi e ruoli tattici dei mob.
- [ ] Difesa della fortezza e interazioni con le macchine.
- [ ] Biomi ostili e anomalie legate all'End.

### Economia e interoperabilità

- [ ] Bilanciamento EE2 e persistenza del valore dell'esplorazione.
- [ ] Audit dei cicli EMC/EU/materiali e delle ricette exploitabili.
- [ ] Integrazione sensata fra energia IC2 e RedPower.
- [ ] Revisione dei colli di bottiglia di crafting inutilmente ripetitivi.

### Endgame

- [ ] Varianti e comportamenti Enderman.
- [ ] Indizi ambientali e contaminazione dell'Overworld.
- [ ] Revisione dell'accesso all'End.
- [ ] Battaglia del drago multi-fase.
- [ ] Bilanciamento delle ricompense e della fantasia di potenza endgame.

**Nota:** le caselle sono aree progettuali, **non** una dichiarazione dello stato reale del codice e non impongono quest'ordine di sviluppo.

## 17. Decisioni ancora aperte

Da risolvere con prototipi o scelte esplicite del project owner:

- Durata definitiva del ciclo lunare e frequenza degli assedi intermedi.
- Formula della progressione della minaccia, suoi cap e comportamento in multiplayer.
- Come identificare una base senza imporre una struttura prestabilita.
- Conseguenze dell'assenza del giocatore o del sonno durante gli assedi.
- Livello ammesso di danno alle costruzioni e protezione dei macchinari.
- Quali biomi diventano ostili e con quali pericoli specifici.
- Come limitare EE2: valori EMC, blacklist, progressione alchemica, catalizzatori o combinazioni moderate.
- Politica delle conversioni EU/Bluetricity/EMC e relative efficienze.
- Esatta difficoltà e pluralità di percorsi per raggiungere l'End.
- Meccaniche dettagliate del drago e conseguenze narrative della vittoria.
- Limiti del crafting da inventari esterni e di altre QoL avanzate.

Un agente **non deve risolvere tacitamente** queste scelte implementando la prima soluzione che trova comoda. Per proseguire su una feature circoscritta, può usare default conservativi, documentandoli come provvisori.

## 18. Istruzioni permanenti per gli agenti LLM

Quando lavori su ZedsTekkitOverhaul:

1. **Leggi questo documento prima di proporre architetture o cambiamenti al gameplay.**
2. Leggi poi la specifica della feature corrente e il codice effettivamente presente nel workspace.
3. Tratta questa visione come vincolo di prodotto; **non** presumere che ogni feature descritta esista già.
4. Distingui intenzioni confermate, proposte e decisioni aperte.
5. Preferisci meccaniche sistemiche, leggibili e integrate a sistemi isolati.
6. Non introdurre grinding, quest obbligatorie, UI invasive o gating senza una motivazione forte e approvata.
7. Prima di scrivere codice, identifica le integrazioni con NEI, Inventory Tweaks, RedPower, EE2, IC2, BuildCraft e i sistemi già aggiunti, **quando pertinenti**.
8. Prevedi contratti ed eventi per le integrazioni future, ma non implementare anticipatamente sottosistemi enormi.
9. Mantieni multiplayer, persistenza, compatibilità legacy e performance come requisiti di base.
10. Nel report finale, spiega **cosa hai implementato, cosa hai deliberatamente lasciato fuori e come la soluzione si allinea a questa visione**.

---

## Sintesi in una frase

> **Una Tekkit Classic fedele al suo spirito di libertà, dove la tecnologia rende possibili grandi imprese, la fortezza ha finalmente una ragione d'esistere, il mondo oppone una sfida reale e il mistero dell'End dà significato alla progressione — senza quest obbligatorie, grinding artificiale o microcrafting inutile.**
