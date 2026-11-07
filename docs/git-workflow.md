# Workflow Git

## Baseline
Importare soltanto `src/minecraft/zed/`, documentazione e script originali.

## Branch
- `main`: build stabile verificata nel gioco.
- `feature/nome`: una funzionalità per branch.
- `fix/nome`: correzioni isolate.

## Commit
Commit piccoli e separati:
- `feat(crafting): add Foundry 5x5 recipe`
- `fix(legacy): correct Container method signature`
- `docs: document build and dependencies`

Prima di ogni commit:
```
git status --short
git add -A
git diff --cached --name-only
git diff --cached
git commit -m "..."
```
Se compare un file terzo inatteso, rimuoverlo dallo staging con
`git restore --staged PERCORSO`, poi correggere `.gitignore`.

## Push
```
git push -u origin main
```

Per nuove funzioni:
```
git switch -c feature/foundry-multiblock
# modifiche, test, commit...
git push -u origin feature/foundry-multiblock
```
Usare GitHub Pull Requests per revisione prima di unire a main.
