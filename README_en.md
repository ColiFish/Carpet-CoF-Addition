# Carpet CoF Addition

[中文](README.md) | **English**

## Requirements

| Dependency | Exact version | Download |
| --- | --- | --- |
| Minecraft | **1.21.4** | — |
| Fabric Loader | **≥ 0.16.9** | [client](https://fabricmc.net/use/installer/) / [server](https://fabricmc.net/use/server/) |
| [Carpet](https://github.com/gnembon/fabric-carpet) | **1.4.161+v241203** (MC 1.21.4 build) | [Modrinth](https://modrinth.com/mod/carpet/versions?g=1.21.4) · [GitHub Releases](https://github.com/gnembon/fabric-carpet/releases) |
| Java | **21** | [Official Oracle download (Java 21)](https://www.oracle.com/java/technologies/downloads/#java21) |

## Installation

1. **Server**: install the [Fabric server](https://fabricmc.net/use/server/), then drop both **Carpet** and this mod into the server's `mods/` folder
2. **Single-player / LAN**: the client needs Fabric + Carpet + this mod too — Carpet is declared as a hard dependency, so a client without it fails to start

## Download

Download this mod from [Releases](../../releases).

You only need to install Java 21 yourself for a **self-hosted server** or when launching with a manually specified Java: the official launcher ships its own runtime, and launchers such as HMCL / PCL can download one automatically.

## Documentation

| Document | Content |
| --- | --- |
| [Rules](docs/en/rules.md) | All 11 rules: description, type, default, options and categories |
| [Commands](docs/en/commands.md) | Syntax and effects of every `/cofdebug` subcommand |

Chinese version: [README.md](README.md) · [docs/zh/](docs/zh/) (the same two documents in Chinese)

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


**Why do my rule changes revert after a restart?**

`/carpet <rule> <value>` only affects the current run. Use `/carpet setDefault <rule> <value>` to store it permanently in the world config, and `/carpet removeDefault <rule>` to drop it.

**Why does Low Price Villagers not affect my existing villagers?**

It only affects trades rolled **after** it is enabled. Villagers with locked-in trades keep their prices; only newly generated offers (new villagers, or a new career tier from levelling up) are priced at the bottom of their range.

**After turning Righteous Companion off, iron golems attack me again - did the rule fail?**

No. Disabling it restores vanilla behaviour immediately: when a nearby villager's reputation for you is <= -100, golems defend the village again. Only while the rule is on do they never attack players on their own.

**I broke the workstation but the villager kept its profession?**

Check whether Novice Villager Trades Don't Reset is on: it only blocks the **spontaneous** refresh caused by the villager losing its own claim; **destroying the workstation block** still clears the profession as in vanilla (replace the block to re-roll trades). If breaking the block does not clear it either, the rule is not behaving as intended.

## Warning

This mod is a vibe-coding product; please use it with caution.
