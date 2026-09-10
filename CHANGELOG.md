# Changelog

All notable changes to the JAUML JSON Utility Library will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2026-09-10

### Added
- **`ConfigFile`**: `asJsonObject()` / `getRoot()` returning a deep clone of the root JSON object.
- **`ConfigFile`**: Path-based accessors — `getByPath`, `setByPath`, `getStringByPath`, `getIntByPath`, `getBooleanByPath`, `getDoubleByPath` (with default-value overloads).
- **`ConfigFile`**: `setBackupRotation(int)` for multi-generation `.bak` rotation on corrupted config recovery.
- **`ConfigFile`**: `reloadIfChanged()` to reload only when file size or mtime changed.
- **`ConfigFile`**: `setStableSave(boolean)` opt-in deterministic save via `JsonLib.stableStringify`.
- **`JaumlConfig`**: `invalidate(String, String)`, `invalidate(Path)`, and `clearCache()` for cache management.
- **`JsonMigrator`**: `setPreferredVersionKey(String)` to control which version key is read and updated during migration.
- **`JsonLib`**: `detectVersion(JsonElement, String preferredKey)` overload checking a preferred key first.
- **`JaumlInitializer`**: `initializeWithDemoConfig()` for explicit demo config workflow; `initialize()` now skips demo config write in production.
- **`JsonSchema`**: Optional constraint keywords — `enum`, `minimum`/`maximum`, `minLength`/`maxLength`, `additionalProperties: false`, and `minItems`/`maxItems`. Schemas that omit these keywords behave exactly as before.
- **`tn.naizo.jauml.util`**: Optional utility package — `PlatformUtil`, `JaumlLogger`, `ModuleVersion`, and `Lifecycle` helpers. Additive only; core config API unchanged.

### Changed
- **Build (Phase 4)**: Explicit `compileOnly` Gson and SLF4J API dependencies in all MultiLoader workspaces; `26.1.2` migrated from `buildSrc` to composite `build-logic` (aligned with `26.2`). See `BUILD.md`.
- **`ConfigFile`**: `save()` now writes atomically via a temp file and `Files.move` (with `ATOMIC_MOVE` when supported).
- **`JaumlInitializer`**: `initialize()` logs compatibility and library version but only writes demo config in development environments or when `-Djauml.writeDemoConfig=true`.
- **Version alignment**: `LIBRARY_VERSION`, all workspace `gradle.properties`, and all `MOD_VERSION` constants bumped to **2.3.0**.
- **README**: Fixed usage example to use `asJsonObject()` and typed getters instead of nonexistent `config.get()`.

### Fixed
- Documentation and migration guide updated for 2.3.0 compatibility matrix while preserving 2.0 API support.

## [2.1.1] - 2026-07-30

### Fixed
- Removed legacy stub `MixinMinecraft` mixin registration that crashed Fabric/NeoForge 26.1.2 when users were still on **1.3.0** jars (`InvalidMixinException: missing an @Mixin annotation`). Fixes [#3](https://github.com/MeherBenSalem/jauml/issues/3) and [#4](https://github.com/MeherBenSalem/jauml/issues/4).

### Changed
- Publish workflow builds all MultiLoader workspaces and uploads every loader jar to Modrinth, CurseForge, and GitHub Releases.

## [2.1.0] - 2026-06-25

### Added
- **`JsonLib`**: Modern JSON utility class providing:
  - `safeParse` for exception-free parsing.
  - `strictParse` to enforce standard RFC 8259 compliance.
  - `stableStringify` for key-sorted deterministic JSON serialization.
  - `deepClone` for clean nested JSON copies.
  - `getByPath` and `setByPath` for dot-bracket path navigation (e.g. `settings.users[0].name`).
  - `merge` to recursively combine configuration layers.
  - `normalize` to apply type coercion and default templates to user configurations.
  - `detectVersion` for finding schema version tags.
- **`JsonSchema`**: Embedded lightweight JSON Schema validator supporting type checks, required keys constraint, object property validation, and array item schema checking.
- **`JsonMigrator`**: Sequence-based directed BFS path-finding migrator for sequential version upgrades.
- **`JsonException`**: Unified, typed exception class for all library errors.
- **`JaumlInitializer`**: Mod-level startup checker verifying configuration health, executing migrations, and performing library/app compatibility checks.
- **JUnit 5 Test Framework**: Shared test suite inside `common-shared` running tests natively across all supported Minecraft versions.
- **Launch Verification script**: PowerShell runner `verify_launch.ps1` to clean, compile, run tests, and package mod jars automatically.

### Changed
- **`ConfigFile`**: Enhanced in-place with:
  - Graceful recovery of corrupted config files (backs up corrupted configs to `.json.bak` and re-creates clean configs from default templates).
  - Implicit type coercion and template normalization during loads.
  - Schema validation check support.
  - Auto-migrations before initialization.
- **`JaumlConfig`**: Added overloaded `open` methods supporting schemas, migrators, and default templates. Added runtime checks (`LIBRARY_VERSION` and `isCompatible`).

### Deprecated
- Legacy static methods in `JaumlConfigLib` are fully preserved but remain deprecated (since 2.0). Applications are encouraged to migrate to instance-based `JaumlConfig.open` methods.
