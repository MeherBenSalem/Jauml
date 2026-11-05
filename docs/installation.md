# Installation

Jauml is a library: it adds nothing to the game by itself and is only needed when a mod you use asks for it. Put the jar that matches your loader and Minecraft version in your `mods` folder, wherever that mod is installed.

## Pick the right file

| Minecraft | Loaders | Java | Also install |
| --- | --- | --- | --- |
| 1.20.1 | Fabric, Forge | 17 | Fabric API (Fabric, recommended) |
| 1.21.1 | Fabric, NeoForge | 21 | Fabric API (Fabric, recommended) |
| 26.2 · 26.3 | Fabric, NeoForge | 25 | Fabric API (Fabric, recommended) |

File names end in the loader and Minecraft version, for example `jauml-fabric-26.3-3.0.0.jar` or `jauml-neoforge-1.21.1-3.0.0.jar`. Each jar declares only its own Minecraft version, so a 26.2 jar won't offer itself on 26.3.

**Minecraft 1.21.11 or 26.1.2?** Use Jauml 2.3.1. Version 3.0 isn't built for those versions.

## Client, server or both

Install Jauml on the same side as the mod that requires it: client, dedicated server or both. Jauml itself sends nothing over the network, so a mismatch between client and server copies of Jauml alone never kicks anyone.

## Fabric API

On Fabric, configs work without Fabric API. Server events and the scheduler need it; without it Jauml logs a warning and those features stay silent. Fabric on 1.20.1 also needs Fabric Loader 0.16.10 or newer.

## Updating from 2.x

Replace the old jar with the 3.0 jar. Config files and the mods that read them carry on as before: 3.0 removes nothing from the 2.x API and changes no method signature. See the [migration guide](../MIGRATION_GUIDE.md) for what is new.

## Where configs live

Each mod chooses a folder inside your `config` directory, for example `config/my_mod/common.json`. Jauml writes one small demo config only in development environments (or with `-Djauml.writeDemoConfig=true`).

## Downloads

- [CurseForge](https://www.curseforge.com/minecraft/mc-mods/jauml)
- [Modrinth](https://modrinth.com/mod/jauml)
- [GitHub releases](https://github.com/MeherBenSalem/jauml/releases)
