# Commands

[中文](../zh/commands.md) | **English**

This mod has exactly one command: `/cofdebug`, used to diagnose "why did this cauldron not freeze" and "was my item consumed".

> The command's output labels are printed **in Chinese** (the mod's development language), so the samples below show the real Chinese output with an English explanation of each row.

## Prerequisites

| | |
| --- | --- |
| Toggled by | Rule `cofDebugCommand` (default `2` = requires OP). Setting it to `false` disables the command entirely, including tab completion |
| Who may use it | Sources with permission level ≥ the rule value: players, the server console and RCON (the latter two are level 4) |
| When it applies | Checked on every execution — **changes take effect immediately**, no restart |
| Where output goes | Command feedback plus the server log (prefixed with `[cof]`), handy for later verification |

```
/carpet cofDebugCommand 2        # requires OP (default)
/carpet cofDebugCommand 0        # anyone may use it (debugging)
/carpet cofDebugCommand 4        # console / RCON only
/carpet cofDebugCommand false    # disabled
```

## Formatting conventions

All output is a two-column "label + value" layout. Results are conveyed by **color alone** (green = satisfied / as expected, red = not satisfied / unexpected, gray = neutral information); rows never carry `✓` / `✗` style markers.

## Command list

| Command | Purpose | Changes the world |
| --- | --- | --- |
| `/cofdebug tick <pos>` | **Step one freeze check manually**, reporting only the counter and the result | Yes (may freeze and drop) |
| `/cofdebug data <pos>` | **Read-only** dump of every condition and the progress at that spot | No (creates nothing) |
| `/cofdebug data <pos> <value>` | Overwrite the stored counter (`0` = clear the record) | Yes (data only) |
| `/cofdebug use <pos> [player]` | Make a player **actually right-click** the block with the held item, reporting before/after | Yes (equivalent to the player doing it) |
| `/cofdebug bucketcheck [player]` | Call vanilla's "what is left in hand" logic to validate the bucket rule | Effectively read-only (simulated on copies) |

---

## `/cofdebug tick <pos>`

Runs one freeze check, as if the random tick had picked this column.

```
/cofdebug tick 8 -59 8
```

```
[cof] /cofdebug tick  8, -59, 8
  步进     1 次结冰检查         (step: one freeze check)
  累计     0 / 1                (progress / threshold)
  本次检查 结冰了               (this check: froze)
```

* `累计` is "current warm-up progress / threshold for the current product" (`0 / 1` forever for `ice`)
* A green `本次检查` means it really froze and dropped a product this time

To test **deterministically** (ignoring chance), suppress the chance gate and step manually:

```
/carpet cauldronFreezingChance 1000000    # disable automatic checks
/cofdebug tick <pos>                      # step manually; it freezes every time
/carpet cauldronFreezingChance 48          # restore afterwards
```

---

## `/cofdebug data <pos>` (read-only)

Prints **every condition** at that position, so you can see at a glance which one fails.

```
/cofdebug data 8 -59 8
```

```
[cof] /cofdebug data  8, -59, 8
  规则     水锅结冰:true  满水要求:true  产物:ice  结冰速率:48
  位置     8, -59, 8
  方块     minecraft:water_cauldron
  水位     3
  水位条件 需要满水
  群系     minecraft:snowy_plains
  降水     SNOW
  露天     是
  方块光   0
  累计     0 / 1
  产物     ice（每 1 次产出 1 个）
  下次结冰 产出产物
```

| Row (Chinese label) | Meaning |
| --- | --- |
| `规则` rules | Current values of the four related rules (red = master switch is off) |
| `位置` position | The cauldron actually being reported. **May differ from the position you typed** — if the typed position is not a cauldron, the command searches up to 4 blocks below that column's surface (for cauldrons covered by a block) |
| `方块` block | Block id at that position (red = not a water cauldron) |
| `水位` water level | **Only printed when the block is a water cauldron** (red = does not satisfy the full-water requirement, or any-level mode is active) |
| `水位条件` level rule | Whether the mod currently requires a full cauldron (`需要满水`) or accepts any level (`任意水位`) |
| `群系` biome | Biome registry id |
| `降水` precipitation | Precipitation vanilla computes for that position (`SNOW` = one of the freeze conditions) |
| `露天` open sky | Whether the sky is visible above |
| `方块光` block light | Block light at that position (green = < 10) |
| `累计` progress | Warm-up progress / threshold |
| `产物` product | Current product setting and "1 per N freezes" |
| `下次结冰` next freeze | Whether the next freeze would produce a drop (`产出产物`) or not (`仍不产出`) |

**This subcommand is read-only throughout**: the progress lookup uses the "read without creating" path, so running it in a world that never warmed up a cauldron creates no data file.

It exists precisely for the three confusing situations — not a cauldron, cauldron without water, cauldron covered by a block — all of which still print the full picture.

---

## `/cofdebug data <pos> <value>` (write)

Overwrites the stored counter, useful to reproduce a mid-warm-up state.

```
/cofdebug data 8 -59 8 2      # set the counter to 2
/cofdebug data 8 -59 8 0      # clear the record for this position
```

```
[cof] /cofdebug data  8, -59, 8
  写入结果 现在累计 2 次        (write result: counter is now 2)
```

* Accepted range: `0` ~ `100000`
* Writing `0` appends "（已清空该位置的记录）" (record cleared) and really deletes the record
* With `packed_ice` / `blue_ice` this lets you push a cauldron straight to "the next freeze produces"

---

## `/cofdebug use <pos> [player]`

Makes the given player (the first online player when omitted) **actually right-click** the block with the held item, then reports what changed. Use it to verify interaction rules such as "does this item get consumed".

```
/player Bot spawn
/gamemode survival Bot
/tp Bot 6.5 -60 8.5
/item replace entity Bot weapon.mainhand with minecraft:potion[potion_contents={potion:"minecraft:water"}]
/cofdebug use 8 -59 8
```

```
[cof] /cofdebug use  8, -59, 8
  执行者   Bot
  手持物品 minecraft:potion x1
  交互后   minecraft:potion x1
  水瓶消耗 否
  方块变化 minecraft:cauldron → minecraft:water_cauldron
```

| Row | Meaning |
| --- | --- |
| `执行者` actor | The player who actually performed it (the first online player when the argument is omitted) |
| `手持物品` held before | What was in hand **before** the interaction |
| `交互后` held after | What is in hand **after** the interaction |
| `水瓶消耗` bottle consumed | **Only shown when a water bottle was held**: `否` = the bottle survived, `是` = it was replaced by a glass bottle |
| `方块变化` block change | Block id before and after (gray = unchanged) |

Notes:

* This is a **real interaction**: it really changes the block and really consumes items — it is not a simulation
* The target must be within the player's interaction range, otherwise the server silently refuses (symptom: neither the item nor the block changes)
* Test the bucket / bottle rules in **survival**: creative never consumes items, so nothing is observable there
* With no player online it reports `失败 没有在线玩家可供交互（可先 /player Bot spawn）` (no online player to interact)

---

## `/cofdebug bucketcheck [player]`

Calls vanilla's `ItemUtils#createFilledResult` — the method that decides what is left in hand — for both the **place water** and **pour into cauldron** paths, validating the bucket rule. Real items are never touched.

```
/cofdebug bucketcheck
```

```
[cof] /cofdebug bucketcheck
  执行者        Bot
  规则          waterBucketNotConsumed = false
  期望          minecraft:bucket x1（按当前规则）
  放水          minecraft:bucket x1
  倒进锅        minecraft:bucket x1
  对照·岩浆桶   minecraft:lava_bucket x1
  对照·空桶装水 minecraft:water_bucket x1
```

| Row | Meaning |
| --- | --- |
| `期望` expected | What the current rule value implies: rule on = still `water_bucket`, rule off = becomes `bucket` |
| `放水` place water / `倒进锅` pour into cauldron | Actual results of both paths (green = matches expectation) |
| `对照·岩浆桶` control: lava bucket | Lava buckets must not be affected by this rule (guards against collateral damage) |
| `对照·空桶装水` control: empty bucket filling | Filling an empty bucket from a cauldron must not be affected |

In creative mode the "infinite materials" branch is used, so the numbers do not represent survival; those rows are shown in gray with a hint.

---

## Combined workflows

**Diagnose "why does this cauldron not freeze"**

```
/cofdebug data <pos>                      # find the red row
/cofdebug tick <pos>                      # step once to confirm the logic itself works
```

**Verify the product cadence**

```
/carpet cauldronFreezingProduct blue_ice
/cofdebug data <pos> 2                    # push straight to "next freeze produces"
/cofdebug tick <pos>                      # should drop blue ice
/cofdebug data <pos>                      # confirm the record was reset
```

**Verify the water bottle rule (survival)**

```
/carpet waterBottleNotConsumed true
/cofdebug use <pos>                       # the 水瓶消耗 row: 否 = working
/carpet waterBottleNotConsumed false
/cofdebug use <pos>                       # again: 是 = vanilla behavior
```
