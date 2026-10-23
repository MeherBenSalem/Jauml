# JAUML roadmap

Where JAUML stands after 3.0.0 and what would make it the one library every NightBeam mod depends on. Ranked by how much duplicated code it removes from the mods (survey of 21 mod repos, October 2026) and by what popular library mods offer (Architectury, Balm, Puzzles Lib, Fzzy Config, YACL, Resourceful Lib, Framework).

## Done in 3.0.0

| Area | What replaced what |
|------|--------------------|
| Platform | `Platform` + `Services` replace the MultiLoader `Services`/`IPlatformHelper` copy found in 15 repos |
| Server runtime | `Scheduler.queueServerWork` replaces the MCreator tick work queue in 6 repos; `ServerEvents`/`PlayerEvents` cover CivilizationAI's `IPlatformEvents` |
| Configs | `TypedConfig` covers the hand-rolled Gson configs (MiniMaps, Mob-Loot-Bags, Ascendant-Mobs 1.21.1, RPG's `IConfigService` clone, NightStats) |
| Utilities | `Cooldowns` replaces ad-hoc `Map<UUID, Long>` cooldowns (9 files in Bosses); `Version`/`VersionRange` for optional integrations |

## Next (3.1): networking

The biggest remaining gap. 9 repos write packet code and 4 (Bosses, Ballistics, fine_tuned_weaponry, Modern-Building-Wands) each built their own common-side `INetworkHelper`; 11 of ~20 popular libraries ship a networking layer.

- One API in common: register clientbound/serverbound payloads with a codec and handler; `sendToServer`, `sendToPlayer`, `sendToTracking`.
- Needs per-version code: 1.20.1 uses `FriendlyByteBuf` channels (Forge `SimpleChannel`, Fabric `PacketByteBufs`); 1.21.1 and 26.x use `CustomPacketPayload` + `StreamCodec`.
- Build in the NeoForge 21.1.229 security guidance: bounded collection codecs (`maxSize`), small serverbound payloads, validation before decoding.

## Then (3.2): config sync and screens

- Server-to-client sync of `TypedConfig` values (needs networking). Fzzy Config, Balm, owo and MezzConfig (now required by JEI) all do this.
- Config screen: hook NeoForge's built-in config screen and Mod Menu on Fabric before writing any GUI. Hand-written widgets churn every version (26.1 render-state rename, 26.2 GUI reorganization, 26.3 SDL text input).
- `/jauml reload` command for typed configs.

## Later

| Feature | Evidence | Note |
|---------|----------|------|
| Registration helpers (deferred holders, creative tabs) | 13 repos with holders, 12 with creative tabs; Ballistics' `Registrar`/`RegistryHolder` is the best starting point | `ResourceLocation` (1.20.1, 1.21.1) vs `Identifier` (26.x) means per-version common code |
| Keybinding and client tick/HUD helpers | 6 repos | Use `InputConstants` on 26.3; raw GLFW codes are gone |
| Player/entity data attachments | fine_tuned_weaponry, RPG, core_utils, Elementals | Fabric data attachment API, NeoForge `AttachmentType`, Forge capabilities |
| Text/colour helpers | No shared helper anywhere yet | 26.2 removed most of `ChatFormatting`; build on `Style` |

## Housekeeping

- Move the mods off local `libs/*.jar` copies of JAUML (Bosses, better_farming_plus, Ascendant-Mobs, RPG) to the Modrinth maven (`maven.modrinth:jauml:<version>`), as fine_tuned_weaponry already does.
- Port the mods to the 3.0 APIs: drop the `Services` copies, the `queueServerWork` queues and the cooldown maps.
- Add a production boot check like Bosses' `tools/prod-boot` that starts real servers with each jar and asserts that `ServerEvents` fire.
