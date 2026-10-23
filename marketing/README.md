# Marketing assets: Jauml 3.0

Listing copy and artwork for Modrinth and CurseForge. Nothing here ships in the mod jars.

## Listing

| File | Use |
| --- | --- |
| [`curseforge/description.md`](curseforge/description.md) | Full project description (Markdown; works on Modrinth and CurseForge) |
| [`curseforge/summary.txt`](curseforge/summary.txt) | Short project summary (197 characters) |
| [`../CHANGELOG.md`](../CHANGELOG.md) | Release notes; the publish workflow uses the top section |

Image links in the description point at `raw.githubusercontent.com/MeherBenSalem/jauml/main/marketing/images/...`, so they work once this folder is on `main`.

The publish workflow's **sync project** option (on by default for tag pushes) sets the Modrinth page's body from `curseforge/description.md`, its summary from `curseforge/summary.txt`, and its icon from `images/icon.png`.

## Images

| File | Size | Use |
| --- | --- | --- |
| `images/banner.png` | 1800×600 | Description header |
| `images/icon.png` | 512×512 | Project avatar: the `{J}` mark on the kit background |
| `images/card-configs.png` | 1600×900 | `BossConfig.java` beside the file Jauml wrote for it |
| `images/card-events.png` | 1600×900 | Server events and the scheduler, with a real self-test log |
| `images/card-platform.png` | 1600×900 | `Services.load` and `Platform` checks |
| `images/card-json.png` | 1600×900 | The 2.x JSON toolkit: migrations, schema, dot paths |
| `images/card-utilities.png` | 1600×900 | `Cooldowns`, `Version` and `VersionRange` |
| `images/card-versions.png` | 1600×900 | The eight jars: Minecraft version × loader |
| `images/header-*.png`, `images/divider.png` | 1400 wide, transparent | Section headings for the description |

### Where the content comes from

Jauml has no gameplay to screenshot, so the cards show its code and output, and nothing on them is mocked up:

- `source/sample/BossConfig.java` is compiled against the built 3.0.0 jar, and `source/sample/bosses.json` is the file `TypedConfig` wrote for it (`Generate.java`). The configs card prints both.
- `source/sample/selftest-26.3-neoforge.log` holds the `JaumlSelfTest` lines from a NeoForge 26.3 GameTest server boot with `-Djauml.selfTest=true`. The card drops the logger name and level from each line so it fits.
- The snippets on the events, platform, JSON and utilities cards (in `render.cjs`) use the real 3.0.0 API. Check them against `common-shared/src/main/java` when the API changes.

To regenerate `bosses.json` after a config change, build the jars, then compile and run the sample against one of them (with Gson and SLF4J from the Gradle cache on the classpath):

```bash
javac -cp "dist/jauml-fabric-26.3-3.0.0.jar;gson.jar;slf4j-api.jar" -d build/sample marketing/source/sample/*.java
java  -cp "build/sample;dist/jauml-fabric-26.3-3.0.0.jar;gson.jar;slf4j-api.jar" Generate marketing/source/sample/bosses.json
```

## Regenerating the artwork

The banner, icon, cards and headers are HTML rendered with Playwright from the files above and the fonts in `source/fonts/`:

```bash
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs                    # everything
NODE_PATH="$(npm root -g)" node marketing/source/render.cjs banner icon        # selected assets
CHROMIUM_PATH=".../chrome.exe" node marketing/source/render.cjs                # a Chromium other than Playwright's default
```

- `VERSIONS`, `LOADERS` and `RELEASE` at the top of `source/render.cjs` set the banner pills and the version matrix. Keep them in step with the compatibility table in the description.
- `source/art/logo.png` is the previous painted logo (the mod's `icon.png` in the 1.20.1 Forge resources), kept for reference; the new icon is drawn in HTML so it stays sharp at 512 px.

## Fonts

The artwork uses Barlow Condensed (display), Inter (text) and JetBrains Mono (code), all under the SIL Open Font License 1.1. `source/fonts/` holds the Latin WOFF2 subsets from Fontsource with their license texts (`barlow_condensed_ofl.txt`, `inter_ofl.txt`, `jetbrains_mono_ofl.txt`). The fonts are only used to render these images; the mod does not bundle them.

## Wording

Jauml is a library: say what it gives mod developers and tell players they only need it when a mod asks for it. Every feature, version and number in the listing comes from the 3.0.0 source, `CHANGELOG.md` and the build; check them there before changing the copy.
