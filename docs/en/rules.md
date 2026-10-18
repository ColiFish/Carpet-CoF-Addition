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

## Villager Trade No Price Hike (reputationNoPriceHike)

Villager trade prices never exceed their original price.

- **Negative reputation**: vanilla turns reputation into a price modifier and negative reputation
  raises prices; this rule only allows it to lower them.
- **Restocking (demand)**: every restock accumulates `demand` for trades that were used a lot, making
  frequently traded villagers charge more over time; this rule caps the final price at the base price,
  so demand can no longer raise it.
- **Discounts** such as Hero of the Village still apply (prices can only get cheaper).
- Both directions are covered (you buying from the villager and the villager buying from you), because
  the rule caps the single place where the final price is computed.

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


## Flammable Gunpowder (flammableGunpowder)

Gunpowder can ignite fire just like a fire charge.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Nitwit Conversion (nitwitConversion)

An adult villager fed a poisonous potato while weakened becomes a nitwit: it **immediately gives up its workstation
claim** and its trades are cleared, while its biome style is untouched. A nitwit fed a golden apple while under
Regeneration turns back into an **unemployed** villager - trades cleared, experience zeroed, profession level reset
to 1, and only the biome style kept. Both directions take 3-5 minutes and can be repeated; whether the villager ever
held a profession or traded makes no difference.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Instant villager conversion (instantVillagerConversion)

Curing zombie villagers and nitwit conversion happen instantly, with no waiting time.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`

## Library of Alexandria (libraryOfAlexandria)

Librarians can sell enchanted books of every kind; the three newly added ones are priced as treasure enchantments.

- Type: `boolean`
- Default: `false`
- Options: `false`, `true`
- Categories: `CoF`, `survival`, `feature`
