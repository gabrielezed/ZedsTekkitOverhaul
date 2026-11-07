# Build e deploy automatico su PrismLauncher (Windows)

Questo workflow usa il **workspace FoxyRetroMDK / MCP 6.2 esistente** e non contiene binari di Minecraft, Forge o Tekkit.

## Prerequisiti

- JDK 8 con jar.exe nel PATH (lo stesso ambiente usato da MCP).
- \`MDK-1.2.5\` configurato con \`recompile.bat\` e \`reobfuscate.bat\`.
- Sorgenti in \`src/minecraft/zed/\` e texture \`src/minecraft/zed/gui/advanced_table.png\`.
- Istanza PrismLauncher **Tekkit Dev** presente e Minecraft chiuso durante il deploy.

## Un comando

Apri PowerShell dalla root \`MDK-1.2.5\`:

\`\`\`powershell
.\zto-tools\build-and-deploy.bat
\`\`\`

Lo script:

1. Esegue la compilazione MCP.
2. Pulisce **soltanto** \`reobf/minecraft/zed/\` per evitare classi obsolete, poi esegue la reobfuscation.
3. Crea \`dist-zto/ZedsTekkitOverhaul.jar\` con tutte le classi \`zed/\` e la texture della GUI.
4. Esegue un backup di un'eventuale versione precedente in \`dist-zto/ZedsTekkitOverhaul.previous.jar\`, quindi copia il JAR nell'istanza Prism.

### Destinazione predefinita

\`\`\`text
%APPDATA%\PrismLauncher\instances\Tekkit Dev\minecraft\mods
\`\`\`

Corrisponde al percorso \`C:\Users\zed\AppData\Roaming\PrismLauncher\instances\Tekkit Dev\minecraft\mods\` nella configurazione attuale.

Per cambiare destinazione solo nella sessione PowerShell corrente:

\`\`\`powershell
$env:ZTO_PRISM_MODS = 'D:\PrismLauncher\instances\Tekkit Dev\minecraft\mods'
.\zto-tools\build-and-deploy.bat
\`\`\`

## Precauzioni

- La directory mods deve **già esistere**. Lo script non crea un'istanza Prism nuova.
- Se trova \`ZedsTekkitOverhaul-0.6.0.jar\` o altri vecchi \`ZedsTekkitOverhaul*.jar\`, interrompe il deploy: spostali fuori da \`mods\` per non caricare due copie della mod.
- Non modifica \`minecraft.jar\`, \`modpack.jar\`, IC2, EE2 o BuildCraft.
- Il deploy di questa mod **non include** eventuali Java agent/patch runtime richiesti da feature GUI sperimentali: sono componenti separati.
- Se compilazione, reobfuscation o packaging falliscono, non sovrascrive il JAR dell'istanza.
- Confronta i valori di ID blocco/config con quelli del tuo mondo prima di avviare un salvataggio esistente.
- Non è stata eseguita una compilazione MCP Windows sul tuo PC da questo repository: il primo deploy va verificato nel tuo ambiente.
