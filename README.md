# Carpet CoF Addition

Carpet 附属模组（Minecraft 1.21.4 / Fabric）：让**装满水的炼药锅在会下雪的环境里结冰**。

结冰时**锅被清空**，并在**锅的中心掉出 1 个冰块**。

> **本模组不注册任何新方块或物品。** 没有新 ID、没有新贴图、没有掉落表；
> 卸载后存档里不会留下"未定义方块"。也不需要 Fabric API。

---

## 依赖

| 依赖 | 版本 |
| --- | --- |
| Fabric Loader | ≥ 0.16.9 |
| Carpet | 1.21.4 版（1.4.161 上验证通过） |
| Java | 21 |

**不需要 Fabric API**（早先为了加载方块模型/掉落表才需要它，现在没有那些资源了）。

### 从源码构建

仓库里**不含第三方 jar**（`libs/*.jar` 被忽略），所以克隆下来后先准备 Carpet 本体：

1. 从 Modrinth / CurseForge 下载 MC 1.21.4 版的 Carpet `1.4.161`
2. 放进项目根目录的 `libs/` 下，文件名必须与 `gradle.properties` 里的 `carpet_jar` **完全一致**：

   ```
   libs/fabric-carpet-1.21.4-1.4.161+v241203.jar
   ```

然后构建：

```bash
gradle clean build -PnoFabricRepo      # 本机装了 Gradle 8.12 时
./gradlew clean build -PnoFabricRepo   # 用仓库自带的 wrapper（首次会下载 Gradle 8.12）
```

产物在 `build/libs/`：`Carpet CoF Addition-<版本>.jar`（装进游戏的那个）与 `-sources.jar`（源码归档）。

---

## 规则（默认全部关闭）

| 规则 | 默认 | 说明 |
| --- | --- | --- |
| `waterCauldronFreezing` | `false` | 总开关：水炼药锅能否结冰 |
| `cauldronFreezingRequiresFullWater` | `true` | 是否只有装满水（水位 3）才结冰；关掉后水位 1~3 都能结冰 |
| `cauldronFreezingProduct` | `ice` | **产出**：`ice` 每次都产出冰块 / `packed_ice` 每 2 次产出 1 个浮冰 / `blue_ice` 每 3 次产出 1 个蓝冰 |
| `cauldronFreezingChance` | `48` | 结冰速率：每次迭代通过概率 **1/N**，N 就是这个值。48 = 与原版水源结冰同节奏，越小越快。**取值不受限**（任意正整数），`/carpet` 里显示的 `Options` 只是建议值（`strict = false`） |
| `cofDebugCommand` | `2` | 是否启用 `/cofdebug` 诊断命令：`false` = 完全关闭；`0`~`4` = 启用且要求该权限等级。**改完立刻生效**，无需重启 |
| `waterBucketNotConsumed` | `false` | 生存模式下用水桶放水 / 倒进炼药锅时不消耗水桶，也不产生空桶 |
| `waterBottleNotConsumed` | `false` | 生存模式下使用水瓶往炼药锅灌水不消耗 |

用 `/carpet <规则名> <值>` 开关。一条规则可以同时属于**总分类**与**子分类**，`Tags:` 里会全部列出：

| 分类 | 包含规则 |
| --- | --- |
| `CoF` | 全部 6 条（本模组总分类，`/carpet list CoF` 列出所有） |
| `feature` | 全部 6 条 |
| `survival` | `waterBucketNotConsumed` |
| `command` | `cofDebugCommand` |
| `creative` | `cofDebugCommand` |

子分类直接沿用 Carpet 自己的分类词汇，所以 `/carpet list feature` 能把本模组的规则和 Carpet 本体的一起筛出来；
这些分类的显示名由 Carpet 自带语言文件提供，本模组不需要额外的 `carpet.category.*` 键。

### 翻译键（加规则时别漏）

| 键 | 作用 |
| --- | --- |
| `carpet.rule.<规则名>.name` | **Carpet 界面用的翻译名**。值是**纯中文名**，Carpet 会自己补上 ` (规则名)`，所以 `/carpet list CoF` 与 `/carpet <规则名>` 显示为 `炼药锅结冰 (waterCauldronFreezing)` |
| `carpet.rule.<规则名>` | 供**第三方工具搜索**的中文名，值写 `中文名(英文名)` —— [Carpet Org Addition](https://github.com/fcsailboat/Carpet-Org-Addition) 的 `/ruleSearch` 靠它（实测 `/ruleSearch 结冰` 能搜到 4 条） |
| `carpet.rule.<规则名>.desc` | 规则描述，`/carpet <规则名>` 显示（中文） |
| `carpet.category.CoF` | 分类标签（`Tags: [CoF]`） |

现有中文名：`炼药锅结冰(waterCauldronFreezing)` / `炼药锅结冰产物(cauldronFreezingProduct)` / `炼药锅结冰需要满水(cauldronFreezingRequiresFullWater)` / `炼药锅结冰速率(cauldronFreezingChance)` / `水桶不消耗(waterBucketNotConsumed)` / `水瓶不消耗(waterBottleNotConsumed)` / `CoF调试工具(cofDebugCommand)`。
**新增规则时三个键都要补**（`.name` / 无后缀 / `.desc`），否则要么界面不翻译、要么搜不到。

---

## 结冰条件（四项同时满足）

1. **不要求当前正在下雪** —— 条件完全由位置决定
2. **在可降雪群系**：`Biome#getPrecipitationAt == SNOW`
3. **降雪能落到这个位置**：锅必须在该列地表（`MOTION_BLOCKING` 高度图），且头顶露天
4. **该位置方块光 < 10**（与原版水结冰一致，看的是方块光，所以白天不阻碍）

## 结冰结果

```
满水锅 → 结冰 → 锅变回原版空锅 + 锅中心掉出 1 个冰块
```

水是**真的被消耗掉**的（不是凭空刷冰，和原版水源结冰一样）。冰块掉在锅中心、会自然落到锅底，
玩家走过去即可拾取。

## 产出（规则 `cauldronFreezingProduct`）

规则值决定"**每产出 1 个需要满足几次结冰**"。**每次产出后计数清零**，所以要拿下一个产物就得重新预热：

| 产物 | 第 1 次 | 第 2 次 | 第 3 次 | 第 4 次 | 节奏 |
| --- | --- | --- | --- | --- | --- |
| `ice`（默认） | **冰块** | 冰块 | 冰块 | 冰块 | 每次都给 |
| `packed_ice` | 不产出 | **浮冰** | 不产出 | 浮冰 | 每 2 次给 1 个 |
| `blue_ice` | 不产出 | 不产出 | **蓝冰** | 不产出 | 每 3 次给 1 个 |

* **预热阶段**不产出任何东西，而且**水保持不动** —— 锅仍是满水锅，下一次检测会继续累计
* **产出的那一次**会把锅清空（水被消耗，同原版水源结冰）；玩家重新灌水后进入下一轮预热
* 预热进度按**方块坐标**持久化（`world/data/cof_cauldron_freezes.dat`），而且**只记录"预热进行中"的位置**
  —— 产出即清零（记录被直接删掉），所以数据量天然很小
* `ice` 的阈值是 1、计数毫无作用，因此这种产物**完全不碰计数与存档数据**（不会创建数据文件）
**预热一旦被打断就作废**，覆盖所有中断方式：

| 中断方式 | 清零时机 |
| --- | --- |
| 产出（锅被清空） | 立即（本来就清零） |
| 挖掉炼药锅 | **立即**（挂在 `Block#playerDestroy`） |
| 空桶舀水 / 玻璃瓶装水 | **立即**（挂在 `AbstractCauldronBlock#useItemOn`）。判据与规则联动：**满水模式**下水位一降就作废；**任意水位模式**下只有水被取光才作废 |
| 换成岩浆锅 / 细雪锅 / 空锅 / 其它方块 | 下次随机刻抽到该列时（通常几分钟内） |

判据统一是"**这口锅现在还结得了冰吗**"，而不是"水位有没有变"：满水规则开着时，水位掉到 3 以下就不再结冰，
进度作废；任意水位模式（`cauldronFreezingRequiresFullWater=false`）下，只要还有水就仍能结冰，进度保留。
随机刻那条清理规则用的是同一个判据。

所有清理都用"只读不创建"（`getIfPresent`），所以从没冻过任何锅的世界不会产生这个数据文件；
所有清理也都只在事件发生时或随机刻顺手做一次，**不碰任何热路径**。

切换与查看（游戏内还会列出可选值）：

```
/carpet cauldronFreezingProduct packed_ice
/carpet cauldronFreezingProduct            # 显示当前值与 Options
/cofdebug data <坐标>                     # 查询该位置已累计几次
/cofdebug data <坐标> <值>                # 改写（0 = 清零）
```

> 这个规则早期是布尔开关（`true`/`false`）。改成这个产出规则后，存档里残留的旧值会被 Carpet 判为非法、
> 自动回落到默认的 `ice`（每次都产出冰块），所以升级不会出问题。
## 结冰速率：与原版水源结冰逐位一致

原版 `ServerLevel#tickChunk` 的降水循环是：

```java
for (int i = 0; i < randomTickSpeed; i++)      // 默认 3 次，不是 16
    if (this.random.nextInt(48) == 0)          // 再乘 1/48 概率门
        this.tickPrecipitation(...);
```

也就是**每列每 tick 被检测到的概率是 1/4096**，期望约 **205 秒**轮到一次。
本模组复刻了同一个循环与概率门（`cauldronFreezingChance` 就是那个分母），
唯一区别是去掉了外面 `if (isRaining())` 那一层。`randomTickSpeed` 设为 0 时和原版一样完全不检测。

> 早先版本写成"每区块 tick 抽查 4 列"（= 1/64，**比原版快 64 倍**），
> 那就是"结冰速率明显异常"的原因。

---

## 为什么不用新方块（关于"是否违背 Carpet 理念"）

Carpet 官方 README 把本体与附属分得很清楚：本体是"把原版已有行为变成可调规则"，
而**附属**才是放"更离谱功能"的地方（官方自己的 `carpet-extra` 就有自动合成、发射器放方块等），
并明确鼓励任何人用 `CarpetExtension` 写自己的功能。所以加方块不违背理念 —— 但**不加更好**：

* **零注册项** = 零存档风险（卸载后不会留下未定义方块），也不会影响其它模组的注册表
* 结冰结果"锅清空 + 掉出冰块"对应原版已有的行为类别：**方块因环境变化而消失并掉落**（如失去支撑的火把/花）
* 判定条件与节奏完全对齐原版**水源结冰**

---

## 诊断命令（`/cofdebug`）

受规则 `cofDebugCommand` 控制（默认 `2`，即需要 OP；`false` 则命令不存在）。

| 命令 | 作用 | 改世界？ |
| --- | --- | --- |
| `/cofdebug tick <坐标>` | **步进一次**结冰检查，只报告累计与本次结果 | ✅ |
| `/cofdebug data <坐标>` | **只读**复述该坐标处的完整条件：规则 / 位置 / 方块 / 水位 / 水位条件 / 群系 / 降水 / 露天 / 方块光 / 累计，外加产物与下次结冰；**不是水炼药锅（别的方块、空锅、被盖住）也照样完整输出** | ❌ |
| `/cofdebug data <坐标> <值>` | 改写该位置的累计次数（`0` = 清除记录） | ✅ |
| `/cofdebug use <坐标> [玩家]` | 让指定玩家（默认第一个在线玩家）用手上物品对该方块**真右键一次**，播报前后变化；手持水瓶时额外给出「水瓶消耗」判定，用来验证 `waterBottleNotConsumed` | ✅ |
| `/cofdebug bucketcheck [玩家]` | 直接调用原版决定"手上剩下什么"的方法，含两个对照 | ❌ |

结果**回显给执行者**（玩家或 RCON 控制台都能直接读到，不依赖「有玩家在线」），同时以纯文本写入**服务端日志**。

---

## 产物

只有一个 jar：**`Carpet CoF Addition-<版本>.jar`**（约 28 KB）。

```
gradle build -PnoFabricRepo
```

诊断命令 `/cofdebug` **始终包含在内**，不需要额外开关（也不再区分"纯功能版 / 调试版"）。
另有一个 `-sources.jar`，只对本项目开发有用，玩家不需要。

---

## 踩过的坑（记录一下）

| 现象 | 原因 |
| --- | --- |
| 结冰比原版快 64 倍 | 概率循环拍脑袋写成"每区块 tick 抽查 4 列"，没复刻原版 1/48 概率门 |
| 无玩家在线时怎么都不结冰 | 区块**不会 tick**（`forceload` 也不提供随机刻），测试必须挂一个 Carpet 假人 |
| 客户端日志里出现假的"方块注册失败"警告 | 客户端入口与 Carpet 的 `onGameStarted` 都**早于注册表引导**，在那里检查必然误报 |
| 方块有贴图、物品没贴图 | 1.21.4 需要 `assets/<ns>/items/<名称>.json`（物品模型定义），只写 `models/item/` 不够 |
| 水桶规则开着时手上的桶会消失 | `createFilledResult` 会消耗输入栈；放水是"取空桶 + 填充结果"两步链，必须整条模拟才能测出来 |
| 以为 Carpet 不使用规则名翻译 | 其实是键少写了 `.name`。Carpet 的 `RuleHelper.translatedName()` 用格式串 `%s.rule.%s.name` 取值（值是纯中文名，括号里的英文名由它自己补）；也正因为格式串里**没有** `carpet.rule` 这个字面量，直接搜 jar 会得出"Carpet 不读这个键"的**错误**结论 |
| 规则值后面多出一个孤立的 `]` | Carpet 对**建议值为空**的规则会无条件打印收尾的 ` ]`（前缀 `Options: [ ` 却只在非空时打印）。给 int 规则补一个 `options` 列表 + `strict = false` 即可：显示正常，取值仍不受限 |

（其中"新方块/物品模型"相关条目现在已成历史 —— 本模组已不再注册任何内容。）

---

## 如何测试（开发时用）

结冰依赖**区块 tick**，所以：

1. 必须有玩家或 Carpet 假人在**区块模拟距离**内 —— 只 `forceload` 不会产生检测。测试时挂一个假人：

   ```
   /player Bot spawn
   /tp Bot <锅旁边>
   ```

2. 锅要在**会下雪的群系、露天、方块光 < 10**。

3. 想**确定性**触发（不看概率），先压掉概率门，再用调试命令手动步进：

   ```
   /carpet cauldronFreezingChance 1000000     # 屏蔽自动检测
   /cofdebug tick <坐标>                       # 手动步进一次
   /cofdebug data <坐标>                       # 只读查看全部条件与进度
   /carpet cauldronFreezingChance 48           # 测完改回
   ```

4. `randomTickSpeed=0` 时完全不检测（刻意与原版一致）。

用 RCON 自动化测试时的几条经验（本项目就是这样测的）：

- 服务端控制台/RCON 的权限等级是 4，所以默认 `cofDebugCommand=2` 下命令可用。
- `execute if block … run say X` 的结果**不会出现在 RCON 响应里**（它走聊天与日志）。要么读日志，要么改用 `/cofdebug data` 读方块与水位。
- `/cofdebug use <坐标> [玩家]` 是验证"手上物品有没有被消耗"的通用工具；手持水瓶时会直接给出「水瓶消耗」判定。
- 假人与方块的距离要在交互范围内，否则服务端会静默拒绝（表现：物品与方块都没变）。
- 区块卸载时 `setblock` 会报 `Could not set the block` —— 先 `forceload add <x> <z>`，或保证假人在线。
- 交互需要**生存模式**才有意义：创造模式下原版本来就不消耗物品，测不出规则是否生效。

---

## 新增一条规则的做法（checklist）

1. 在 `CofSettings.java` 加字段：`@Rule(categories = {CATEGORY, …}) public static <类型> 名字 = 默认值;`
   - 取值受限就用 `options = {…}`；想"只给建议、不限制取值"再加 `strict = false`
   - 分类沿用 Carpet 自己的词汇：`feature` / `survival` / `command` / `creative`（中文显示名由 Carpet 自带语言提供，本模组不用另配）
2. **三个语言键都要补**（`assets/cof/lang/zh_cn.json` 与 `en_us.json`）：
   - `carpet.rule.<名>.name` —— Carpet 界面用，值写**纯中文名**（Carpet 自己会补上 ` (规则名)`）
   - `carpet.rule.<名>` —— 供第三方工具搜索（例如 Carpet Org Addition 的 `/ruleSearch`），值写 `中文名(英文名)`
   - `carpet.rule.<名>.desc` —— 规则描述
3. 在 `README.md` 的规则表加一行（标识符 + 默认值 + 说明）
4. 需要诊断手段就在 `CofDebugCommand` 加子命令，并登记到 README 的诊断命令表；若新增了 mixin，记得写进 `cof.mixins.json`
5. `gradle clean build -PnoFabricRepo` 构建，进游戏 `/carpet list CoF` 确认名称、描述、分类都正常
6. `CHANGELOG.md` 记一笔，`gradle.properties` 提 `mod_version`，打 tag 发 Release

---

## 构建

```bash
gradle build -PnoFabricRepo      # 只产出一个 jar：Carpet CoF Addition-<版本>.jar
```

（`-PnoFabricRepo` 只是禁用官方 Fabric 仓库兜底，正常构建都可以带上。产物固定为单个 jar，
诊断命令 `/cofdebug` 始终包含在内；开关它请用规则 `cofDebugCommand`。）

构建脚本中与国内网络相关的几处处理，见 `build.gradle` 顶部注释。
