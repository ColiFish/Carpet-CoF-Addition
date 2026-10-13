# 规则

**中文** | [English](../en/rules.md)

**提示：可以使用 `Ctrl+F` 快速查找自己想要的规则。**

```text
/carpet <规则名> <值>              # 临时修改（重启后恢复）
/carpet <规则名>                   # 查询描述、当前值与可选值
/carpet setDefault <规则名> <值>    # 永久修改
/carpet removeDefault <规则名>      # 取消永久设置
/carpet list CoF                   # 列出本模组全部规则
```

---

## 炼药锅结冰 (waterCauldronFreezing)

允许下雨天/雪天让水炼药锅自然结冰。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `feature`

## 炼药锅结冰产物 (cauldronFreezingProduct)

含水炼药锅结冰后的产物，每产出1个需要满足以下结冰次数：ice = 每次都给冰块、packed_ice = 每2次给1个浮冰、blue_ice = 每3次给1个蓝冰，每次产出后计数清零。

- 类型: `string`
- 默认值: `ice`
- 参考选项: `ice`, `packed_ice`, `blue_ice`
- 分类: `CoF`, `feature`

## 炼药锅结冰需要满水 (cauldronFreezingRequiresFullWater)

含水炼药锅是否必须满水位才结冰。

- 类型: `boolean`
- 默认值: `true`
- 参考选项: `false`, `true`
- 分类: `CoF`, `feature`

## 炼药锅结冰速率 (cauldronFreezingChance)

结冰检测频率，48 ≈ 原版水源结冰，数值越小越快。

- 类型: `int`
- 默认值: `48`
- 分类: `CoF`, `feature`

## 水桶不消耗 (waterBucketNotConsumed)

生存模式下使用水桶不消耗。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 水瓶不消耗 (waterBottleNotConsumed)

生存模式下使用水瓶往炼药锅灌水不消耗。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## CoF调试工具 (cofDebugCommand)

是否启用 /cofdebug 诊断命令，0~4为启用且只有权限等级达到该数字的玩家可用。

- 类型: `string`
- 默认值: `2`
- 参考选项: `false`, `0`, `1`, `2`, `3`, `4`
- 分类: `CoF`, `command`, `creative`, `feature`

## 低价村民 (lowPriceVillagers)

村民出售的商品按价格区间下限出售，只影响新刷出的交易。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 声望不涨价 (reputationNoPriceHike)

村民因声望调价时只能降价或不变。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 正义的伙伴 (righteousCompanion)

铁傀儡永不主动攻击玩家：不再因村庄声望出手，也不会在被玩家攻击后还手，开启后正在追击玩家的傀儡会立刻收手，但关闭后会立即受村庄声望影响。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 新手村民交易不重置 (noviceVillagerTradeNotReset)

新手村民不会因自己弄丢工作站而重随机交易。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`


## 可燃火药 (flammableGunpowder)

火药像烈焰弹一样可以点火。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 傻子可逆转化 (nitwitConversion)

成年村民在虚弱状态下被喂毒马铃薯会变成傻子；傻子在再生状态下被喂金苹果会变回原职业，职业、经验与交易列表都会保留，可随时来回转化。

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`

## 亚历山大图书馆 (libraryOfAlexandria)

图书管理员可以出售所有种类的附魔书，新增的三种按宝藏附魔计算价格

- 类型: `boolean`
- 默认值: `false`
- 参考选项: `false`, `true`
- 分类: `CoF`, `survival`, `feature`
