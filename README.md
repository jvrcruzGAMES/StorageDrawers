# StorageDrawers

## Original project by WanMine, patched to work with Hytale release 4
### Original project's curseforge page: [here](https://www.curseforge.com/hytale/mods/wans-storage-drawers)

- All rights reserved to WanMine

A Hytale server mod built with Java.

## Building

```bash
./gradlew shadowJar
```

The output JAR will be in `build/libs/`.

## Deploying

```bash
./gradlew deployMod
```

Builds the fat JAR and copies it to the Hytale server `mods/` folder.

## Running

Use the included run configurations in your IDE:

- **Run Hytale Server** — Builds, deploys, and starts the server
- **Debug Hytale Server** — Same as Run, with remote debugger on port 5005
- **Build Mod** — Compiles without deploying or starting the server
