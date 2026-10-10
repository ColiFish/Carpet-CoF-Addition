# Carpet CoF Addition

[中文](README.md) | **English**

## Requirements

| Dependency | Exact version | Download |
| --- | --- | --- |
| Minecraft | **1.21.4** | — |
| Fabric Loader | **≥ 0.16.9** | <https://fabricmc.net/use/installer/> |
| [Carpet](https://github.com/gnembon/fabric-carpet) | **1.4.161+v241203** (MC 1.21.4 build) | [Modrinth](https://modrinth.com/mod/carpet/versions?g=1.21.4) · [GitHub Releases](https://github.com/gnembon/fabric-carpet/releases) |
| Java | **21** | [Temurin 21 (Windows x64 JDK)](https://adoptium.net/temurin/releases/?version=21&os=windows&arch=x64&package=jdk) · [Microsoft OpenJDK 21](https://learn.microsoft.com/java/openjdk/download) |

Download this mod from [Releases](../../releases), or build it yourself as described in the [development docs](docs/en/development.md).

You only need to install Java 21 yourself for a **self-hosted server** or when launching with a manually specified Java: the official launcher ships its own runtime, and launchers such as HMCL / PCL can download one automatically.

## Documentation

| Document | Content |
| --- | --- |
| [Rules](docs/en/rules.md) | All 7 rules: defaults, accepted values, conditions, examples, caveats |
| [Commands](docs/en/commands.md) | `/cofdebug` syntax, arguments, sample output and combined workflows |
| [Development](docs/en/development.md) | Building from source, testing, adding a rule, implementation notes and pitfalls |

Chinese version: [README.md](README.md) · [docs/](docs/zh/) (the same three documents in Chinese)

## FAQ

**The rule is on but the cauldron never freezes.**

All four conditions must hold at once: snow-capable biome, the cauldron sits on the column surface with open sky above, block light < 10, and enough water (full by default). On top of that, **a player or a Carpet fake player must be within simulation distance** — loading chunks with `forceload` alone never triggers the check. `/cofdebug data <pos>` shows which condition fails.

**Why is everything off by default?**

That is the convention for Carpet-based mods: features are opt-in so existing worlds and servers are never affected silently.

**Does it create ice out of nothing?**

No. The water is **actually consumed**: on the producing freeze the cauldron is emptied, exactly like vanilla water freezing.

**Why does it not freeze on stony shores?**

Freezing follows the vanilla **snow line**: the check uses `Biome#getPrecipitationAt(pos, seaLevel)`, which corrects temperature by height. Below the snow line vanilla reports rain, so no freezing happens there — moving the cauldron up or down across the snow line changes the outcome.

**The bucket / bottle rules seem to do nothing in creative.**

Creative mode never consumes items, so those two switches make no visible difference. Test them in survival.

**What does it leave in my world?**

One data file, `world/data/cof_cauldron_freezes.dat` (one per dimension), and it only stores positions that are **currently warming up** — producing, breaking or emptying a cauldron clears the record immediately. A world that never warmed up a cauldron never gets the file at all.

## License

Released under [GPL-3.0](LICENSE) (GPL-3.0-only). Copyright (c) 2026 CoF contributors.
