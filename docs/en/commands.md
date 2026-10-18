# Commands

[中文](../zh/commands.md) | **English**

The mod adds exactly one command: `/cofdebug`, for inspecting the live state of each rule - for example "why is this cauldron not freezing" or "was my item consumed". Availability and permission level are controlled by the `cofDebugCommand` rule (default `2`, usable by OPs; `false` disables the command completely).

---

## Freeze check (`/cofdebug tick`)

### Syntax
- `/cofdebug tick <pos>`
    - `<pos>` position of the target block

### Effects
- Steps one freeze check manually and reports the target, the counter and the result.
- **Modifies the world**: it may actually freeze and produce the product.
- Refused while the master switch (`waterCauldronFreezing`) is off.

## POI diagnostics (`/cofdebug poi`)

### Syntax
- `/cofdebug poi <pos>`
    - `<pos>` the workstation block to inspect

### Effect
- **Read-only**: reports the POI (workstation) state at that position and changes nothing.
- Reported lines:
    - Block: which block is actually there;
    - POI type: the registry id (e.g. `minecraft:weaponsmith`), or "not any POI";
    - Occupied: `used / max` - workstations usually allow 1, so this reads `1 / 1` or `0 / 1`;
      the few types with a higher limit show "occupied"/"free" instead;
    - Free: remaining slots, `0` means nobody else can bind it; types with a higher limit
      show "max N" instead;
- **Side effect (never printed)**: every villager within 128 blocks whose `job_site` points here
  receives a **10 s glowing** effect, so you can see in-game which villager holds the claim.
- Typical use: when you suspect a workstation is stuck claimed and no other villager can bind it -
  `1 / 1` with no villager glowing means the ticket leaked; `0 / 1` means the ticket is clean and
  the villager simply has not walked over to claim it yet (vanilla claiming is slow).

## Data inspection (`/cofdebug data`)

### Syntax
- `/cofdebug data <pos>`
    - `<pos>` position of the target block

### Effects
- **Read-only** dump of every condition and the progress at that spot; it never creates a data file.
- Reported rows: current rule values, the position actually reported, dimension, block, water level, level rule, biome, precipitation, open sky, block light, counter, product and whether the next freeze produces anything.
- **The reported position may differ from the one you typed**: the cauldron the freeze logic would actually handle is preferred (that column surface / one below the surface); only when none is found does it probe up to 4 blocks down.

## Data write (`/cofdebug data` write)

### Syntax
- `/cofdebug data <pos> <value>`
    - `<pos>` position of the target block
    - `<value>` the counter to store, `0` clears the record

### Effects
- Overwrites the stored counter on the same cauldron the freeze logic uses.
- When the position does not line up it falls back to the literal position and says so, so you never "write successfully but read nothing".
- **Modifies data only** (nothing freezes).

## Item interaction (`/cofdebug use`)

### Syntax
- `/cofdebug use <pos> [player]`
    - `<pos>` position of the target block
    - `[player]` the player who acts; the first online player when omitted

### Effects
- Makes that player **actually right-click** the block with the held item and reports the change.
- Reported rows: actor, held item before, held item after, whether the bottle was consumed (only when a water bottle was held) and the block change.
- **Modifies the world** (identical to a player action).

## Bucket check (`/cofdebug bucketcheck`)

### Syntax
- `/cofdebug bucketcheck [player]`
    - `[player]` the player used for the simulation; the first online player when omitted

### Effects
- Calls vanilla logic for "what is left in hand" to validate the `waterBucketNotConsumed` rule.
- Reported rows: expectation, the results of both paths (pouring out and pouring into a cauldron) and two controls (lava bucket, filling an empty bucket).
- Uses copies, so it is **effectively read-only**; skipped entirely in creative mode.

## Reputation query (`/cofdebug reputation`)
/cofdebug reputation clear [radius] [player]   # clear the gossip ledger of villagers within the radius (default 32, 1-256): omit the player to clear it for every player, pass one to clear only theirs

### Syntax
- `/cofdebug reputation [player]`
    - `[player]` the player to query; the executing player when omitted

### Effects
- **Read-only** display of that player's reputation with the nearest villager (within 64 blocks).
- Reported rows: the villager (name and position), the reputation, and whether the iron golem defence threshold is reached (prints "yes" or "no"; the threshold is reputation <= -100).
- Reputation is the weighted sum of the gossip ledger; weights are `major_negative` -5, `minor_negative` -1, `minor_positive` +1, `major_positive` +5 and `trading` +1. Per-entry detail is available via `/data get entity <villager> Gossips`.
- Debugging tip: when several villagers are left in the world the query may land on a different one, so clear the area before testing related behaviour.
