# Rules

[中文](../zh/rules.md) | **English**

This mod ships **7 rules**. The feature switches (freezing, water buckets, water bottles) are off by default, so installing it changes nothing; the parameter rules (product, full-water requirement, rate) have their own defaults; and the debug command `cofDebugCommand` defaults to `2` (usable by OPs). Everything is toggled with Carpet's own commands — no second command set to remember.

```
/carpet <rule> <value>             # temporary (reverts on restart)
/carpet <rule>                     # print description, current value and options
/carpet setDefault <rule> <value>   # permanent
/carpet removeDefault <rule>        # drop the permanent setting
/carpet list CoF                   # list every rule of this mod
```

> Rule names and descriptions ship with English translations (`en_us`), so on an English client `/carpet` shows English text. In-game command *output* from `/cofdebug` is printed in Chinese (see [Commands](../en/commands.md)).

## Overview

| Rule | Default | Accepted values | Categories |
| --- | --- | --- | --- |
| `waterCauldronFreezing` | `false` | `true` / `false` | `CoF` `feature` |
| `cauldronFreezingProduct` | `ice` | `ice` / `packed_ice` / `blue_ice` | `CoF` `feature` |
| `cauldronFreezingRequiresFullWater` | `true` | `true` / `false` | `CoF` `feature` |
| `cauldronFreezingChance` | `48` | any positive integer (6 suggested values in the UI) | `CoF` `feature` |
| `waterBucketNotConsumed` | `false` | `true` / `false` | `CoF` `survival` `feature` |
| `waterBottleNotConsumed` | `false` | `true` / `false` | `CoF` `survival` `feature` |
| `cofDebugCommand` | `2` | `false` / `0` ~ `4` | `CoF` `command` `creative` `feature` |

A rule can belong to the mod's own category and to Carpet's sub-categories at the same time; `Tags:` lists all of them. The sub-categories reuse Carpet's own vocabulary, so `/carpet list feature` shows this mod's rules together with Carpet's.

---

## waterCauldronFreezing — master switch

**Overworld only**: the Nether, the End and custom dimensions are never handled (water there has no vanilla precipitation semantics).

| | |
| --- | --- |
| Default | `false` |
| Type | boolean |
| Categories | `CoF` `feature` |

**In-game description:** in snow-capable biomes, a full water cauldron that can see the sky and sits in block light below 10 freezes and drops a product at its center.

With this off, the core mechanic does nothing and the other rules become meaningless.

### The four freeze conditions

1. **It does not have to be snowing right now** — everything is decided by position
2. **Snow-capable biome**: `Biome#getPrecipitationAt(pos, seaLevel) == SNOW` (height-corrected, i.e. it follows the vanilla snow line)
3. **Snow can reach the spot**: the cauldron must sit on that column's surface (`MOTION_BLOCKING` heightmap) and see the sky above
4. **Block light < 10 at that position** (same as vanilla water freezing; it is *block* light, so daylight does not interfere)

### Tick cadence

Hooked into `ServerLevel#tickChunk`, replicating the structure and the 1/48 chance gate of vanilla's precipitation loop:

```java
for (int i = 0; i < randomTickSpeed; i++)     // 3 by default
    if (this.random.nextInt(<cauldronFreezingChance>) == 0)
        // freeze check
```

So each column is checked with probability randomTickSpeed × (1/256) × (1/48) = 3/12288 = **1/4096** per tick — about once every **205 seconds** with defaults. With `randomTickSpeed = 0` nothing is checked, exactly like vanilla.

**A player or a Carpet fake player must be within simulation distance**: loading a chunk with `forceload` alone never runs `tickChunk`.

### Product and drop position

* On the finishing freeze the **cauldron is emptied** (the water is really consumed) and one product drops at the **cauldron's center**
* The dropped item's collision box rests **flush on the cauldron's inner floor** (`y + 0.25`), so it never falls through

### Examples

```
/carpet waterCauldronFreezing true
/carpet cauldronFreezingProduct blue_ice
/carpet setDefault waterCauldronFreezing true      # permanent
```

---

## cauldronFreezingProduct — what a cauldron produces

| | |
| --- | --- |
| Default | `ice` |
| Accepted values | `ice` / `packed_ice` / `blue_ice` |
| Categories | `CoF` `feature` |

**Note**: warm-up progress stores only a count, not which rules produced it — switching the product tier (`packed_ice` ↔ `blue_ice`) or the full-water requirement mid-way keeps applying existing counts under the new rules. A cauldron fully covered by blocks is not checked at all, so its progress is kept until it becomes reachable again.

**In-game description:** what a water cauldron produces, and how many freeze events are needed per item: ice = every time, packed_ice = 1 per 2, blue_ice = 1 per 3. The counter resets after every drop.

### Warm-up cadence

The value decides **how many freeze events are needed to produce one item**. **The counter resets after each drop**, so the next item requires a fresh warm-up:

| Product | 1st | 2nd | 3rd | 4th | Cadence |
| --- | --- | --- | --- | --- | --- |
| `ice` (default) | **ice** | ice | ice | ice | every time |
| `packed_ice` | nothing | **packed ice** | nothing | packed ice | 1 per 2 |
| `blue_ice` | nothing | nothing | **blue ice** | nothing | 1 per 3 |

* **During warm-up**: nothing drops, and the **water stays put** (the cauldron remains full, the next check keeps counting)
* **On the producing freeze**: the cauldron is emptied; refill it to start the next warm-up
* `ice` has a threshold of 1, so the counter is meaningless — that setting **touches neither the counter nor the saved data** (no data file is created)

### When warm-up progress is discarded

One rule covers all cases: **can this cauldron still freeze?**

| Interruption | Cleared |
| --- | --- |
| Producing (cauldron emptied) | immediately (it was reset anyway) |
| Breaking the cauldron | **immediately** |
| Scooping with a bucket / filling a glass bottle | **immediately**; the exact criterion depends on the full-water rule below |
| Replaced by a lava / powder snow / empty cauldron or any other block | on the next random tick that picks that column (usually within minutes) |

Progress is persisted **per block position** in `world/data/cof_cauldron_freezes.dat` (one file per dimension), and it **only stores positions that are actively warming up** — producing clears (deletes) the record, so the data stays tiny by construction. Every cleanup path is read-only and never creates the file, so a world that never warmed up a cauldron has none.

### Examples

```
/carpet cauldronFreezingProduct packed_ice
/carpet cauldronFreezingProduct              # show current value and Options
/cofdebug data <pos>                         # how many freezes this spot has counted
/cofdebug data <pos> <value>                 # overwrite the counter (0 = clear the record)
```

---

## cauldronFreezingRequiresFullWater — full water required

| | |
| --- | --- |
| Default | `true` |
| Type | boolean |
| Categories | `CoF` `feature` |

**In-game description:** when on, only a full cauldron freezes; when off, water levels 1–3 all freeze.

This rule also decides what counts as an interrupted warm-up:

| Mode | Water level changes (e.g. 3 → 2, or topping up to 3) | Water taken out entirely |
| --- | --- | --- |
| `true` (default, full) | warm-up is **discarded immediately** | discarded |
| `false` (any level) | warm-up is **unaffected** (any remaining water keeps counting) | discarded |

In other words: full mode demands the level stays untouched, while any-level mode keeps counting as long as the block is still a water cauldron.

---

## cauldronFreezingChance — freeze rate

| | |
| --- | --- |
| Default | `48` |
| Type | integer |
| Suggested values | `1` / `4` / `12` / `48` / `96` / `192` (the UI's `Options`, click to apply) |
| Categories | `CoF` `feature` |

**In-game description:** freeze check frequency; 48 ≈ vanilla water freezing, smaller is faster.

This is the denominator of the chance gate: each iteration passes with probability **1/N**, so a smaller N freezes faster. The default `48` matches vanilla's `random.nextInt(48)` exactly.

**The value is not restricted**: any positive integer works, and the 6 `Options` in the UI are only suggestions (`strict = false`). Invalid values (≤ 0) are treated as 1.

```
/carpet cauldronFreezingChance 12      # 4× faster than default
/carpet cauldronFreezingChance 1       # check on every iteration (debugging)
```

---

## waterBucketNotConsumed — water buckets are not consumed

| | |
| --- | --- |
| Default | `false` |
| Type | boolean |
| Categories | `CoF` `survival` `feature` |

**In-game description:** using a water bucket in survival does not consume it.

Covers both paths: **placing** water (emptying the bucket onto the ground) and **pouring it into a cauldron**. With it on you keep the water bucket and no empty bucket appears.

Creative mode never consumes items in the first place, so this switch only makes a visible difference in survival.

---

## waterBottleNotConsumed — water bottles are not consumed

| | |
| --- | --- |
| Default | `false` |
| Type | boolean |
| Categories | `CoF` `survival` `feature` |

**In-game description:** filling a cauldron with a water bottle in survival does not consume it.

Vanilla consumes the bottle and hands the player a glass bottle; with this on the water is still poured in (level +1) but **you keep the water bottle**.

Both vanilla cases that consume a bottle are covered:

| Cauldron before | Vanilla behavior | With this rule on |
| --- | --- | --- |
| Empty cauldron | filled to level 1, bottle consumed | filled to level 1, **bottle kept** |
| Water cauldron, level 1 / 2 | level +1, bottle consumed | level +1, **bottle kept** |
| Water cauldron, full (3) | nothing | nothing |
| Lava / powder snow cauldron | nothing | nothing |

**Drinking is deliberately unaffected**: drinking a potion (`PotionItem#finishUsingItem`) also returns a glass bottle, but that path is never intercepted — otherwise it would become an infinite water bottle exploit.

What you keep is **the original bottle** (data components and custom name included), not a freshly created one.

---

## cofDebugCommand — CoF debug tool

| | |
| --- | --- |
| Default | `2` |
| Accepted values | `false` / `0` ~ `4` |
| Categories | `CoF` `command` `creative` `feature` |

**In-game description:** whether to enable the /cofdebug diagnostic command; 0–4 enables it and requires that permission level.

| Value | Meaning |
| --- | --- |
| `false` | fully disabled: the command does not work and does not even appear in tab completion |
| `0` ~ `4` | enabled, and only sources with permission level ≥ the value may use it (`2` means OP) |

The check runs on every execution, so **changes apply immediately with no restart**. The server console and RCON have permission level 4, so they work with the default setting.

**Only `false` or `0`~`4` are accepted**: an unparsable value or a negative number disables the command outright instead of silently falling back to "requires OP".

---

## Appendix: why this is not a new block

An earlier version really did implement a "frozen cauldron" **block** (with blockstate, model, loot table and tags). All of it was removed in favor of the current **pure logic** implementation, because:

* **Registry pollution**: a new block means a new ID. After a player removes the mod, those blocks in the world become "undefined blocks" — at best error spam, at worst a damaged save
* **Extra assets and dependencies**: once models, textures, loot tables and tags pile up it is no longer a pure-logic mod, and it drags in additional library dependencies
* **It matches how Carpet add-ons usually work**: only changing vanilla behavior, never adding content, is what makes a mod safe to drop onto a production server

That is why freezing looks like "empty the vanilla water cauldron and drop a product at its center" rather than "turn it into a new block" — and why this mod registers nothing at all.
