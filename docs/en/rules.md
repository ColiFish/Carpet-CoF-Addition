# Rules

[中文](../zh/rules.md) | **English**

**Tip: press `Ctrl+F` to find the rule you want.**

```text
/carpet <rule> <value>             # temporary (reverts on restart)
/carpet <rule>                     # print description, current value and options
/carpet setDefault <rule> <value>   # permanent
/carpet removeDefault <rule>        # drop the permanent setting
/carpet list CoF                   # list every rule of this mod
```

---

## Water Cauldron Freezing (waterCauldronFreezing)

Lets water cauldrons freeze naturally in rain or snow.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `feature`

## Freezing Product (cauldronFreezingProduct)

What a filled water cauldron produces and how many freeze events each item needs: ice = ice every time, packed_ice = one packed ice per 2, blue_ice = one blue ice per 3; the count resets after every item.

- Type: `string`
- Default: `ice`
- Options: `ice`, `packed_ice`, `blue_ice`
- Categories: `CoF`, `feature`

## Requires Full Water (cauldronFreezingRequiresFullWater)

Whether a filled water cauldron must be at full water level to freeze.

- Type: `boolean`
- Default: `true`
- Options: `false`, `true`
- Categories: `CoF`, `feature`

## Freezing Rate (cauldronFreezingChance)

Freeze check frequency: 48 matches vanilla water freezing, smaller values freeze faster.

- Type: `int`
- Default: `48`
- Categories: `CoF`, `feature`

## Water Bucket Not Consumed (waterBucketNotConsumed)

Using a water bucket in survival does not consume it.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Water Bottle Not Consumed (waterBottleNotConsumed)

Using a water bottle to fill a cauldron in survival does not consume it.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## CoF Debug Tool (cofDebugCommand)

Whether the /cofdebug diagnostic command is enabled; 0-4 = enabled and only usable by players whose permission level reaches that number.

- Type: `string`
- Default: `2`
- Options: `false`, `0`, `1`, `2`, `3`, `4`
- Categories: `CoF`, `command`, `creative`, `feature`

## Low Price Villagers (lowPriceVillagers)

Villagers sell at the bottom of each trade price range; only newly rolled trades are affected.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## No Reputation Price Hike (reputationNoPriceHike)

Reputation can only lower villager prices, never raise them.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Righteous Companion (righteousCompanion)

Iron golems never attack players on their own: they stop acting on village gossip and no longer retaliate when hit, and any golem already chasing a player gives up immediately - but once the rule is turned off they are affected by village gossip again right away.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Novice Villager Trades Don't Reset (noviceVillagerTradeNotReset)

Novice villagers never re-roll their trades because they lost their workstation on their own.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`
