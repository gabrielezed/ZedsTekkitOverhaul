# Linee guida per agenti LLM

Target: Minecraft Java 1.2.5, MCP 6.2, Forge 1.2.5 e Java 6 bytecode.
- Evitare API moderne (BlockPos, Capability, DeferredRegister, JSON recipes).
- Non modificare o committare `src/minecraft/net/`, `forge/`, `jars/` o JAR delle mod originali.
- Non cambiare le integrazioni EE2/IC2 già funzionanti senza un test specifico.
- Entry point: `src/minecraft/zed/tekkitoverhaul/mod_ZedsTekkitOverhaul.java`.
- Le integrazioni vanno tenute in classi dedicate.
- Per cambiare classi vanilla usare patch testuali versionate o transformer separati; non includere classi vanilla ricompilate.
- Prima di un commit: ispezionare `git diff`, verificare errori Eclipse,
  eseguire `recompile.bat` e, se pertinente, `reobfuscate.bat`.
- Non dichiarare verificato in gioco ciò che è solo compilato o simulato.
- Ogni modifica a ricette, ID numerici o EMC richiede note di compatibilità/salvataggi.
