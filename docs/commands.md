# 命令文档

**中文** | [English](en/commands.md)

本模组只有一套命令：`/cofdebug`，用于诊断"锅为什么不结冰""物品有没有被消耗"。

## 前置

| 项 | 说明 |
| --- | --- |
| 谁来开关 | 规则 `cofDebugCommand`（默认 `2` = 需要 OP）。设为 `false` 时命令**完全不可用**，连补全里也不会出现 |
| 谁能用 | 权限等级 ≥ 规则值的玩家、服务端控制台、RCON（后两者权限等级为 4） |
| 生效时机 | 每次执行时判定，**改完立刻生效**，无需重启 |
| 输出去哪 | 命令回显 + 服务端日志（带 `[cof]` 前缀，方便事后核对） |

```
/carpet cofDebugCommand 2        # 需要 OP（默认）
/carpet cofDebugCommand 0        # 任何人可用（含调试用）
/carpet cofDebugCommand 4        # 只有控制台/RCON 可用
/carpet cofDebugCommand false    # 完全关闭
```

## 排版约定

所有输出都是"标签 + 值"的两列布局，判断结果**只看颜色**（绿 = 满足/符合预期，红 = 不满足/不符预期，灰 = 中性信息），行内不再有 `✓` `✗` 之类的记号。

## 命令一览

| 命令 | 作用 | 改世界 |
| --- | --- | --- |
| `/cofdebug tick <坐标>` | **手动步进一次**结冰检查，只报累计与本次结果 | 会（可能结冰并产出） |
| `/cofdebug data <坐标>` | **只读**复述该坐标的完整条件与进度 | 不会（不创建任何数据） |
| `/cofdebug data <坐标> <值>` | 改写该位置的累计次数（`0` = 清除记录） | 会（只改数据） |
| `/cofdebug use <坐标> [玩家]` | 让指定玩家用手上物品对该方块**真右键一次**，播报前后变化 | 会（等同于玩家自己操作） |
| `/cofdebug bucketcheck [玩家]` | 调用原版"手上最终剩下什么"的判定，验证水桶规则 | 实质只读（用副本模拟，不动真实物品） |

---

## `/cofdebug tick <坐标>`

手动执行一次结冰检查，等价于随机刻抽到了这一列。

```
/cofdebug tick 8 -59 8
```

```
[cof] /cofdebug tick  8, -59, 8
  步进     1 次结冰检查
  累计     0 / 1
  本次检查 结冰了
```

* `累计` 是"当前预热进度 / 本次产物所需阈值"（`ice` 档恒为 `0 / 1`）
* `本次检查` 绿色表示这次真的结冰并产出了

想**确定性**地测试结冰（不看概率），先把概率门压掉、再手动步进：

```
/carpet cauldronFreezingChance 1000000    # 屏蔽自动检测
/cofdebug tick <坐标>                      # 手动步进，每次都会结冰
/carpet cauldronFreezingChance 48          # 测完改回
```

---

## `/cofdebug data <坐标>`（只读）

把该坐标处的**全部条件**逐条报出来，一眼看出卡在哪一项。

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

| 行 | 含义 |
| --- | --- |
| `规则` | 四条相关规则的当前值（红色 = 总开关是关的） |
| `位置` | 实际被报告的那口锅的坐标。**可以不等于你输入的坐标** —— 输入的坐标上不是锅时，会在该列地表往下 4 格内找锅（锅被方块盖住的情况） |
| `方块` | 该位置的方块 id（红 = 不是水炼药锅） |
| `水位` | **只有方块是水炼药锅时才出现**（红 = 不满足满水要求，或当前是任意水位模式） |
| `水位条件` | 当前是"需要满水"还是"任意水位" |
| `群系` | 群系注册表 id |
| `降水` | 该位置由原版算出的降水类型（`SNOW` = 可结冰条件之一） |
| `露天` | 头顶是否见天 |
| `方块光` | 该位置方块光（绿 = < 10） |
| `累计` | 预热进度 / 阈值 |
| `产物` | 当前产物档位及"每几次产出 1 个" |
| `下次结冰` | 再结冰一次会不会产出（`产出产物` / `仍不产出`） |

**这个子命令全程只读**：进度查询走"只读不创建"的路径，所以对从没预热过的世界执行它不会生成数据文件。

它的用途正好覆盖三种"看起来不正常"的情况 —— 不是锅、锅没水、锅被盖住 —— 都能看到完整内容。

---

## `/cofdebug data <坐标> <值>`（写）

改写该位置的累计次数，用来复现"预热到第 2 次"这种中间状态。

```
/cofdebug data 8 -59 8 2      # 累计设为 2
/cofdebug data 8 -59 8 0      # 清除该位置的记录
```

```
[cof] /cofdebug data  8, -59, 8
  写入结果 现在累计 2 次
```

* 值域 `0` ~ `100000`
* 写 `0` 会在值后面追加「（已清空该位置的记录）」并真的删除记录
* 用 `packed_ice` / `blue_ice` 时可以借此直接把锅推到"下一次产出"

---

## `/cofdebug use <坐标> [玩家]`

让指定玩家（省略时为第一个在线玩家）用手上的物品对该方块**真右键一次**，然后播报前后变化。用来验证"物品会不会被消耗"这类交互规则。

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

| 行 | 含义 |
| --- | --- |
| `执行者` | 实际动手的玩家（省略参数时取第一个在线玩家） |
| `手持物品` | 交互**之前**手上拿的东西 |
| `交互后` | 交互**之后**手上剩的东西 |
| `水瓶消耗` | **只有交互前拿的是水瓶时才出现**：`否` = 水瓶保留了，`是` = 被换成了空玻璃瓶 |
| `方块变化` | 交互前后的方块 id（灰色 = 没变化） |

注意事项：

* 这是**真实的交互**，会真的改方块、真的消耗物品 —— 不是模拟
* 目标方块必须在玩家的交互距离内，否则服务端会静默拒绝（表现：物品与方块都没变）
* 想验证水桶/水瓶规则必须在**生存模式**下测：创造模式本来就不消耗物品，看不出差别
* 没有任何玩家在线时会报 `失败 没有在线玩家可供交互（可先 /player Bot spawn）`

---

## `/cofdebug bucketcheck [玩家]`

调用原版决定"手上最终剩下什么"的那个方法（`ItemUtils#createFilledResult`），分别模拟**放水**和**倒进炼药锅**两条路径，验证水桶规则是否按预期工作。真实物品不会被改动。

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

| 行 | 含义 |
| --- | --- |
| `期望` | 按当前规则值应该得到什么：规则开 = 仍是 `water_bucket`，规则关 = 变成 `bucket` |
| `放水` / `倒进锅` | 两条路径的实际结果（绿 = 与期望一致） |
| `对照·岩浆桶` | 岩浆桶不该被本规则影响（防误伤） |
| `对照·空桶装水` | 空桶从锅里装水不该被本规则影响 |

创造模式下会走"无限材料"分支，数字不代表生存结果，这时各行显示为灰色并给出提示。

---

## 组合用法

**排查"这口锅为什么不结冰"**

```
/cofdebug data <坐标>                      # 看十四行里有哪一行是红的
/cofdebug tick <坐标>                      # 手动推进一步，确认逻辑本身能走通
```

**确认产出档位是否正确**

```
/carpet cauldronFreezingProduct blue_ice
/cofdebug data <坐标> 2                    # 直接推到"下一次结冰就产出"
/cofdebug tick <坐标>                      # 应掉出蓝冰
/cofdebug data <坐标>                      # 确认记录已清零
```

**验证水瓶规则（生存模式）**

```
/carpet waterBottleNotConsumed true
/cofdebug use <坐标>                       # 看"水瓶消耗"一行：否 = 生效
/carpet waterBottleNotConsumed false
/cofdebug use <坐标>                       # 再看一次：是 = 原版行为
```
