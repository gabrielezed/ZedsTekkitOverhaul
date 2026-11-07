# Git workflow

## Repository baseline

Track only original `src/minecraft/zed/` source/resources, project documentation and project scripts. Do not commit any of the original Minecraft or Tekkit toolchain.

## Branches

- `main`: last deliberately accepted baseline.
- `feature/<name>`: one functional feature per branch.
- `fix/<name>`: isolated corrections.
- `docs/<name>`: substantive documentation work.

## Commits

Keep commits small and purposeful:

- `feat(crafting): add Foundry 5x5 recipe`
- `fix(legacy): correct Container method signature`
- `docs(agents): document legacy integration boundaries`

Before each commit:

```powershell
git status --short
git diff --check
git add -A
git diff --cached --name-only
git diff --cached
git commit -m "..."
```

If an unexpected third-party file appears in staging, remove it with `git restore --staged PATH` and fix `.gitignore` before committing.

## Push and pull requests

For a new feature, start from up-to-date `main`:

```powershell
git switch main
git pull --ff-only origin main
git switch -c feature/foundry-multiblock
# Make changes, compile and test them.
git push -u origin feature/foundry-multiblock
```

Prefer a GitHub pull request for reviewing features before merging into `main`. Include the test environment, actual checks performed, limitations and compatibility implications.

See [agent workflow](agents/workflow.md) and [validation](agents/testing.md) for handoff requirements. All repository content must be written in English.
