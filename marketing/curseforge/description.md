![Jauml 3.0](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/banner.png)

## The common library behind NightBeam mods

Jauml (Just Another Utility Minecraft Library) holds the code our mods share: typed JSON configs with comments, server events with a tick scheduler, and one platform API for Fabric, NeoForge and Forge.

**Playing?** If a mod asks for Jauml, put the jar for your loader and Minecraft version in your `mods` folder. It adds nothing to the game by itself.

**3.0** is available for **Fabric, NeoForge and Forge** on **1.20.1, 1.21.1, 26.2 and 26.3**. Mods built for Jauml 2.x keep working on it.

![divider](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/divider.png)

![Typed configs](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-configs.png)

![Write a class. Get a config.](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-configs.png)

Write your settings as a plain Java class and bind it with `JaumlConfig.bind("my_mod", "common", MyConfig.class)`.

- **Comments in the file.** `@Comment` text, `@Range` bounds, allowed enum values and defaults are written as `//` lines above each option. Players can add their own comments too; the file still loads.
- **Safe loading.** Missing options get their defaults, values of the wrong type are converted or reset, and numbers outside their range are clamped, each with a warning in the log.
- **Nested sections** from nested classes, plus lists and maps. Map entries a player removes stay removed.
- **Live updates.** `reload()`, `reloadIfChanged()`, `update(...)` and change listeners. A file that breaks while the game runs keeps the last good values; one that breaks before startup is moved to `.bak` and replaced with defaults.
- **Migrations** run before binding, so renamed or moved options carry over.

![Server events](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-events.png)

![Hook the server on every loader](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-events.png)

- **`ServerEvents`:** starting, started, stopping, stopped, tick start and tick end, for dedicated and singleplayer servers. `ServerEvents.currentServer()` returns the running server.
- **`PlayerEvents`:** join and leave.
- **`Scheduler`:** `runLater`, `runRepeating` and `queueServerWork` (the MCreator work queue, same meaning), always on the server thread. Pending tasks are dropped when the server stops.
- One listener works on Fabric, NeoForge and Forge. Start a server with `-Djauml.selfTest=true` to see every event in the log; the card above shows that log from a NeoForge 26.3 boot.

![Platform](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-platform.png)

![One API. Three loaders.](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-platform.png)

- **`Services.load(IMyHelper.class)`** loads your own loader-specific implementations, so you can delete the copy of the MultiLoader `Services` class from your mod.
- **`Platform`:** the loader, physical client or dedicated server, development environment, game and config directories, the Minecraft version, and the version and name of any loaded mod.
- **Version-aware integrations:** `Platform.isModLoaded("geckolib", ">=4.4")` and `Platform.isMinecraft(">=26.2")`.

![JSON toolkit](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-json.png)

![Every config you already have](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-json.png)

The 2.x config API is all still here:

- **`ConfigFile`** via `JaumlConfig.open(...)`: typed getters, dot paths such as `server.port` or `users[0].name`, lists, atomic saves, reload on change and backups of broken files.
- **`JsonSchema`** for type, range, enum, length and required-key checks, **`JsonMigrator`** for versioned upgrades, and **`JsonLib`** for safe parsing, deep merge, normalization and stable output.
- **`JaumlConfigLib`**, the static helpers older mods call, keeps every method and signature for the whole 3.x line.

![Utilities](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-utilities.png)

![The helpers you keep rewriting](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-utilities.png)

- **`Cooldowns`:** per-player (or per-anything) cooldowns on wall-clock time or server ticks.
- **`Version` and `VersionRange`:** compare `1.20.1`, `26.3.0.95-beta` or `0.165.0+26.3`, and check ranges written as `[2.1,3)`, `>=2.1 <3`, `~2.3`, `^2`, `1.x` or `a || b`.
- **`Event`:** a small listener list with priorities for your own events.

![Downloads & compatibility](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-compatibility.png)

![Every version. Every loader.](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/card-versions.png)

| Minecraft | Loaders | Java | Also install |
| --- | --- | --- | --- |
| 1.20.1 | Fabric, Forge | 17 | Fabric API (Fabric, recommended) |
| 1.21.1 | Fabric, NeoForge | 21 | Fabric API (Fabric, recommended) |
| 26.2 · 26.3 | Fabric, NeoForge | 25 | Fabric API (Fabric, recommended) |

- Download the file that matches your loader and Minecraft version, for example `jauml-fabric-26.3-3.0.0.jar`.
- Install it wherever the mod that needs it is installed: client, server or both.
- On Fabric, configs work without Fabric API; server events and the scheduler need it, and Jauml logs a warning when it is missing. Fabric on 1.20.1 needs Fabric Loader 0.16.10 or newer.
- **Updating from 2.x:** replace the old jar. Your config files and the mods that use them carry on as before.
- **Minecraft 1.21.11 or 26.1.2?** Keep Jauml 2.3.1; 3.0 is not built for those versions.

![Getting started](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-start.png)

Jauml is on the Modrinth Maven. Use the version number shown on the Versions tab, which ends in your loader and Minecraft version:

```groovy
repositories {
    maven { url = "https://api.modrinth.com/maven" }
}

dependencies {
    // Fabric 1.20.1 / 1.21.1 and Forge 1.20.1 remap mod jars:
    modImplementation "maven.modrinth:jauml:3.0.0+fabric-1.21.1"
    // NeoForge, and Fabric on 26.x (unobfuscated):
    implementation "maven.modrinth:jauml:3.0.0+neoforge-26.3"
}
```

Then declare it in `fabric.mod.json` (`"jauml": ">=3.0.0"`) or `neoforge.mods.toml` / `mods.toml` (`versionRange = "[3.0.0,)"`). The [migration guide](https://github.com/MeherBenSalem/jauml/blob/main/MIGRATION_GUIDE.md) walks through every API with examples.

![Questions](https://raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/header-faq.png)

**Does Jauml do anything on its own?**
No. It is a library: it loads, writes one small demo config in development environments only, and waits for mods that use it.

**Do I need it on the server and the client?**
Install it wherever the mod that requires it is installed. Jauml itself sends nothing over the network.

**Will my mods that were built for Jauml 2.x still work?**
Yes. 3.0 removes nothing from 2.x and changes no signature; a test in the build pins every method released mods call.

**Where are the configs?**
In your `config` folder, under the folder each mod chose, for example `config/my_mod/common.json`.

**Where are the docs?**
[Source and README](https://github.com/MeherBenSalem/jauml) · [Migration guide](https://github.com/MeherBenSalem/jauml/blob/main/MIGRATION_GUIDE.md) · [Changelog](https://github.com/MeherBenSalem/jauml/blob/main/CHANGELOG.md) · [Roadmap](https://github.com/MeherBenSalem/jauml/blob/main/ROADMAP.md) · [Issues](https://github.com/MeherBenSalem/jauml/issues)

**Where can I talk to you?**
[Discord](https://discord.gg/TJc3CxR4jK) · [YouTube](https://www.youtube.com/@nightbeamstudio) · [More NightBeam mods on Modrinth](https://modrinth.com/organization/nightbeam) and [CurseForge](https://www.curseforge.com/members/nightbeamstudio/projects) · [Support us](https://nightbeam.dev/support)

---

Jauml is licensed under Apache-2.0. Made by NightBeam Studio.
