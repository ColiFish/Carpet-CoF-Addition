# Development

[中文](../zh/development.md) | **English**

For people changing this mod: how to build it, how to test it, how to add rules, and where the traps are.

## Environment

| Dependency | Version |
| --- | --- |
| JDK | 21 |
| Gradle | 8.12 (wrapper shipped in the repo) |
| Loom | 1.9.2 (declared in `build.gradle`) |
| Mappings | Mojang official mappings (mojmap), **not** Yarn |

### Building from source

The repository **does not contain third-party jars** (`libs/*.jar` is git-ignored), so after cloning, prepare Carpet first:

1. Download Carpet `1.4.161` for MC 1.21.4 from Modrinth / CurseForge
2. Put it into `libs/` at the project root; the file name must match `carpet_jar` in `gradle.properties` **exactly**:

   ```
   libs/fabric-carpet-1.21.4-1.4.161+v241203.jar
   ```

Then build:

```bash
gradle clean build -PnoFabricRepo      # with Gradle 8.12 installed locally
./gradlew clean build -PnoFabricRepo   # with the bundled wrapper (downloads Gradle 8.12 once)
```

* `-PnoFabricRepo` only disables the official Fabric repository fallback; it is safe to always pass
* The output is a **single jar**: `build/libs/Carpet CoF Addition-<version>.jar`, with the diagnostic command always included (toggle it with the `cofDebugCommand` rule)
* A `-sources.jar` (source archive, shipped with releases, not installable) is produced as well
* The header of `build.gradle` documents the China-network workarounds (local Maven mirror, timeouts, retries)

## Project layout

| Path | Responsibility |
| --- | --- |
| `CofMod.java` | Mod entry point (hands the extension to Carpet) |
| `CofExtension.java` | Carpet extension: registers rules and commands |
| `CofSettings.java` | **Definitions of the 7 rules** (categories, defaults, options) |
| `CauldronFreezing.java` | Freeze check and product drop (core logic, incl. `threshold()`) |
| `CauldronFreezeData.java` | Persistence of warm-up progress (`SavedData`) |
| `WaterBottles.java` | Water-bottle predicate (shared by the mixin and the debug command) |
| `CofDebugCommand.java` | All `/cofdebug` subcommands and output layout |
| `mixin/ServerLevelMixin.java` | Hooks `ServerLevel#tickChunk`, replicating vanilla's precipitation loop |
| `mixin/AbstractCauldronBlockMixin.java` | Water bottle not consumed + discard warm-up when water is taken |
| `mixin/ItemUtilsMixin.java` | Water bucket not consumed (intercepts `createFilledResult`) |
| `mixin/BlockMixin.java` | Clears the record immediately when a cauldron is broken |
| `resources/cof.mixins.json` | Mixin registration table (**new mixins must be listed here**) |
| `resources/assets/cof/lang/*.json` | Chinese and English rule names / descriptions |

## How to test

Freezing depends on **chunk ticks**, therefore:

1. A player or a Carpet fake player must be within **simulation distance** — `forceload` alone never triggers the check

   ```
   /player Bot spawn
   /tp Bot <next to the cauldron>
   ```

2. The cauldron must be in a **snow-capable biome, under open sky, with block light < 10**
3. For deterministic behavior, suppress the chance gate and step manually:

   ```
   /carpet cauldronFreezingChance 1000000
   /cofdebug tick <pos>
   /carpet cauldronFreezingChance 48
   ```

4. With `randomTickSpeed = 0` nothing is checked at all (deliberately matching vanilla)

### Lessons from automated RCON testing

This is how the mod was tested; a few hard-won lessons:

* The server console / RCON has permission level 4, so commands work with the default `cofDebugCommand=2`
* `execute if block … run say X` output does **not** appear in the RCON response (it goes to chat and the log). Either read the log, or use `/cofdebug data` to read block and water level instead
* `/cofdebug use <pos> [player]` is the general tool for "was the held item consumed"; when a water bottle is held it prints a `水瓶消耗` verdict directly
* The fake player must be within interaction range of the block, otherwise the server **silently refuses** (symptom: neither item nor block changes — easy to misread as a broken feature)
* `setblock` reports `Could not set the block` while the chunk is unloaded — run `forceload add <x> <z>` first, or keep the fake player online
* Interactions only mean something in **survival**: creative never consumes items
* Vanilla lets an empty bucket fill **only from a full (level 3) water cauldron**; at level 1–2 right-clicking does nothing at all — that is not a bug

## Adding a rule

1. Add a field in `CofSettings.java`: `@Rule(categories = {CATEGORY, …}) public static <type> name = default;`
   * Restrict values with `options = {…}`; to make them advisory only, add `strict = false`
   * Reuse Carpet's own categories: `feature` / `survival` / `command` / `creative` (Carpet ships their display names, this mod needs no `carpet.category.*` keys)
2. **Add all three translation keys** (`assets/cof/lang/zh_cn.json` and `en_us.json`):

   | Key | Purpose |
   | --- | --- |
   | `carpet.rule.<name>.name` | Translation used by the Carpet UI. The value is the bare name; Carpet itself appends ` (rule name)` |
   | `carpet.rule.<name>` | Bare key used by third-party search tools — e.g. `/ruleSearch` from [Carpet Org Addition](https://github.com/fcsailboat/Carpet-Org-Addition) |
   | `carpet.rule.<name>.desc` | Rule description shown by `/carpet <rule>` |

   Missing `.name` means an untranslated UI entry; missing the suffix-less key means third-party search cannot find it.
3. Document it in [`docs/zh/rules.md`](../zh/rules.md) (overview table + a section) and in the English [`docs/en/rules.md`](rules.md)
4. Add a diagnostic subcommand to `CofDebugCommand` if useful, and document it in `docs/zh/commands.md`; remember to list new mixins in `cof.mixins.json`
5. `gradle clean build -PnoFabricRepo`, then check `/carpet list CoF` in game for name, description and categories
6. Note it in `CHANGELOG.md`, bump `mod_version` in `gradle.properties`, tag and publish a release
7. New **files** must carry the SPDX header too: `// SPDX-License-Identifier: GPL-3.0-only` plus `// Copyright (c) 2026 CoF contributors`

## Implementation notes

* **Zero registration**: no new blocks, items, recipes or textures. Everything is an opt-in change to vanilla behavior, so removing the mod leaves no undefined blocks behind
* **Freezing hooks `ServerLevel#tickChunk`**: it replicates vanilla's loop exactly (`randomTickSpeed` iterations × `nextInt(48) == 0`), so the cadence matches vanilla precipitation freezing
* **Progress uses `SavedData`**: stored per dimension (`world/data/cof_cauldron_freezes.dat`) as `long[]` positions plus `int[]` counters. Only actively warming-up positions have records, and producing deletes them, which keeps the data naturally tiny
* **Read paths never create the file**: `getIfPresent` is used by every query and cleanup path; only `get` creates it. A world that never warmed up a cauldron therefore has no data file
* **Why the water bottle rule hooks HEAD and cancels the method**: vanilla consumes the bottle first, then swaps the hand item to a glass bottle, then changes the block. By the time the method returns, the passed-in stack is already **air**, so it cannot be restored afterwards and there is no way to tell what it used to be
* **Why it does not intercept `ItemUtils#createFilledResult`**: drinking a potion goes through the same method and also returns a glass bottle; intercepting it would hand out infinite water bottles

## Pitfalls

| Symptom | Cause and fix |
| --- | --- |
| Cauldrons never freeze after `forceload` | Loading chunks does not run `tickChunk`; a player or fake player must be within simulation distance |
| `/carpet cauldronFreezingChance` renders a stray `]` | A Carpet rendering quirk for integer rules with empty `options`; give it `options` plus `strict = false` |
| `/carpet list CoF` shows untranslated rule names | Carpet reads `carpet.rule.<name>.name`, not `carpet.rule.<name>` — and appends `(rule name)` itself |
| Chinese search in `/ruleSearch` finds nothing | That key is the suffix-less `carpet.rule.<name>`, with the value written as `ChineseName(englishName)` |
| The water bottle rule does not apply | The empty-cauldron case was missed: vanilla fills an empty cauldron to level 1 with a water bottle |
| `setblock` keeps reporting `Could not set the block` in the dev world | The chunk is not loaded (fake player gone / no forceload), not a command syntax problem |
| Intercepting `createFilledResult` for "water bottles are not consumed" | Drinking a potion uses the same method; doing so creates an infinite-bottle exploit |

## Releasing

1. Bump `mod_version` in `gradle.properties`
2. `gradle clean build -PnoFabricRepo`
3. Commit, push, and tag `v<version>` (matching `mod_version`)
4. Attach `dist/Carpet CoF Addition-<version>.jar` to a GitHub release (optionally the `-sources.jar` too)
5. Write the release notes using the sections from `CHANGELOG.md` (新内容 / 修复 / 设计与实现); **do not use emoji**
