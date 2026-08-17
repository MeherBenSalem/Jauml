# Contributing to Jauml

Thank you for your interest in contributing.

## Getting started

1. Fork [MeherBenSalem/jauml](https://github.com/MeherBenSalem/jauml) on GitHub.
2. Clone your fork locally.
3. Create a branch for your change: `git checkout -b fix/my-change`.

## Making changes

- Shared library code belongs in [`common-shared/`](common-shared/).
- Loader-specific code goes in the appropriate workspace under `fabric/`, `forge/`, or `neoforge/`.
- Follow existing naming and package structure (`tn.naizo.jauml`).
- Keep changes focused — one logical change per pull request.

## Testing

Run tests for the workspace you modified:

```powershell
cd 1.21.1
.\gradlew.bat test
```

To verify all workspaces:

```powershell
.\verify_launch.ps1
```

Tests live in `common-shared/src/test/java` and run through each workspace's `common` module.

## Submitting a pull request

1. Ensure builds and tests pass for affected workspaces.
2. Update [`CHANGELOG.md`](CHANGELOG.md) under `[Unreleased]` if the change is user-facing.
3. Open a pull request against `main` with a clear description of what changed and why.
4. Link related issues when applicable.

## Reporting issues

Use [GitHub Issues](https://github.com/MeherBenSalem/jauml/issues) for bugs and feature requests. Include:

- Minecraft version and loader (Fabric, Forge, NeoForge)
- Jauml version
- Steps to reproduce
- Relevant logs or stack traces

## Contribution licensing

By submitting a pull request, you agree that your contributions are licensed under the [Apache License 2.0](LICENSE), the same license that covers this project, unless you explicitly state otherwise in your contribution.
