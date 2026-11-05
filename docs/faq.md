# FAQ

Answers to the questions players and mod developers ask most about Jauml.

## Does Jauml do anything on its own?

No. It is a library: it loads, writes one small demo config in development environments only, and waits for mods that use it.

## Do I need it on the server and the client?

Install it wherever the mod that requires it is installed. Jauml itself sends nothing over the network.

## Will mods built for Jauml 2.x still work?

Yes. 3.0 removes nothing from 2.x and changes no signature; a test in the build pins every method released mods call. See [Compatibility](compatibility.md).

## I'm on Minecraft 1.21.11 or 26.1.2

Use Jauml 2.3.1. Version 3.0 isn't built for those versions.

## A mod says Jauml events aren't firing on Fabric

Install Fabric API. Server events and the scheduler use it; configs work without it. Jauml logs a warning at startup when it is missing.

## Where are the config files?

In your `config` folder, under the folder each mod chose, for example `config/my_mod/common.json`.

## My config file was replaced with defaults

It couldn't be read at startup, so Jauml moved it to `<name>.json.bak` and wrote a fresh one. Compare the two, fix the broken line in the backup (often a missing comma or quote) and copy your values back. See [Typed configs](typed-configs.md#safe-loading).

## Can I add my own comments to a config?

Yes, in typed configs: `//` comments load fine. Plain `ConfigFile` configs opened with `JaumlConfig.open(...)` are strict JSON.

## Where do I report a bug?

On [GitHub issues](https://github.com/MeherBenSalem/jauml/issues) or in the bug reports forum on our [Discord](https://discord.gg/TJc3CxR4jK). Include your Minecraft version, loader, Jauml version and `latest.log`.
