# L2 Imagine Breaker Dispell

`l2_imaginebreakerdispell` is a small Minecraft Forge 1.20.1 addon mod.

Its purpose is to make Imagine Breaker also bypass Dispell, similar to how it already bypasses Dementor in L2hostility.

Current status: scaffold only. No gameplay logic or compatibility fix is implemented yet.

## Build / Run

```powershell
.\gradlew.bat runClient
.\gradlew.bat build
```

```bash
./gradlew runClient
./gradlew build
```

## Repo Layout

- `src/main/java/com/doug/imaginebreakerdispell/`: main mod entrypoint and compat stub
- `src/main/resources/`: Forge metadata and minimal assets
- `docs/`: small project notes and file tree
