# 开发记录

> 这是本模组的**内部开发记录**（不列入 README 的文档索引），面向以后改代码的自己与贡献者。

面向改这个模组的人：怎么构建、怎么测、怎么加规则、哪里容易踩坑。

## 环境

| 依赖 | 版本 |
| --- | --- |
| JDK | 21 |
| Gradle | 8.12（仓库自带 wrapper） |
| Loom | 1.9.2（`build.gradle` 里声明） |
| 映射 | Mojang 官方映射（mojmap），不是 Yarn |

### 从源码构建

仓库**不含第三方 jar**（`libs/*.jar` 被 `.gitignore` 忽略），克隆下来后先准备 Carpet 本体：

1. 从 Modrinth / CurseForge 下载 MC 1.21.4 版的 Carpet `1.4.161`
2. 放进项目根目录的 `libs/`，文件名必须与 `gradle.properties` 里的 `carpet_jar` **完全一致**：

   ```
   libs/fabric-carpet-1.21.4-1.4.161+v241203.jar
   ```

然后构建：

```bash
gradle clean build -PnoFabricRepo      # 本机装了 Gradle 8.12
./gradlew clean build -PnoFabricRepo   # 用仓库自带的 wrapper（首次会下载 Gradle 8.12）
```

* `-PnoFabricRepo` 只是禁用官方 Fabric 仓库兜底，正常构建都可以带上
* 产物固定为**单个 jar**：`build/libs/Carpet-CoF-Addition-<版本>.jar`，诊断命令始终包含在内（用规则 `cofDebugCommand` 开关）
* 另外会产出一份 `-sources.jar`（源码归档，随 Release 分发用，不能装进游戏）
* `build.gradle` 顶部注释记录了与国内网络相关的几处处理（本地 Maven 镜像、超时与重试）

## 项目结构

| 路径 | 职责 |
| --- | --- |
| `CofMod.java` | 模组入口（把扩展交给 Carpet） |
| `CofExtension.java` | Carpet 扩展：注册规则、注册命令 |
| `CofSettings.java` | **7 条规则的定义**（分类、默认值、可选值） |
| `CauldronFreezing.java` | 结冰判定与产出（核心逻辑，含 `threshold()`） |
| `CauldronFreezeData.java` | 预热进度的存档持久化（`SavedData`） |
| `WaterBottles.java` | 水瓶判定（mixin 与调试命令共用一份） |
| `CofDebugCommand.java` | `/cofdebug` 全部子命令与输出排版 |
| `mixin/ServerLevelMixin.java` | 挂 `ServerLevel#tickChunk`，复刻原版降水循环 |
| `mixin/AbstractCauldronBlockMixin.java` | 水瓶灌水不消耗 + 取水即作废预热 |
| `mixin/ItemUtilsMixin.java` | 水桶不消耗（拦 `createFilledResult`） |
| `mixin/BlockMixin.java` | 挖掉炼药锅时立即清记录 |
| `resources/cof.mixins.json` | mixin 登记表（**新增 mixin 必须写进来**） |
| `resources/assets/cof/lang/*.json` | 规则名与描述的中英译文 |

## 如何测试

结冰依赖**区块 tick**，所以：

1. 必须有玩家或 Carpet 假人在**区块模拟距离**内 —— 只 `forceload` 不会产生检测

   ```
   /player Bot spawn
   /tp Bot <锅旁边>
   ```

2. 锅要在**会下雪的群系、露天、方块光 < 10**
3. 想确定性触发，先压掉概率门再手动步进：

   ```
   /carpet cauldronFreezingChance 1000000
   /cofdebug tick <坐标>
   /carpet cauldronFreezingChance 48
   ```

4. `randomTickSpeed=0` 时完全不检测（刻意与原版一致）

### 用 RCON 自动化测试的经验

本项目就是这样测的，几条踩出来的经验：

* 服务端控制台/RCON 权限等级是 4，默认 `cofDebugCommand=2` 下命令可用
* `execute if block … run say X` 的结果**不会出现在 RCON 响应里**（它走聊天与日志）。要么读日志，要么改用 `/cofdebug data` 读方块与水位
* `/cofdebug use <坐标> [玩家]` 是验证"物品有没有被消耗"的通用工具，手持水瓶时会直接给出「水瓶消耗」判定
* 假人与方块的距离要在交互范围内，否则服务端**静默拒绝**（表现：物品与方块都没变，容易误判成功能失效）
* 区块卸载时 `setblock` 会报 `Could not set the block` —— 先 `forceload add <x> <z>`，或保证假人在线
* 交互必须**生存模式**才有意义：创造模式本来就不消耗
* 原版的空桶**只能从满水锅（水位 3）装水**，水位 1~2 时右键毫无反应 —— 这不是 bug

## 新增一条规则

1. 在 `CofSettings.java` 加字段：`@Rule(categories = {CATEGORY, …}) public static <类型> 名字 = 默认值;`
   * 取值受限就用 `options = {…}`；想"只给建议、不限制取值"再加 `strict = false`
   * 分类沿用 Carpet 自己的词汇：`feature` / `survival` / `command` / `creative`（中文显示名由 Carpet 自带语言提供，本模组不用另配）
2. **三个语言键都要补**（`assets/cof/lang/zh_cn.json` 与 `en_us.json`）：

   | 键 | 作用 |
   | --- | --- |
   | `carpet.rule.<名>.name` | Carpet 界面用的翻译名。值是**纯中文名**，Carpet 会自己补上 ` (规则名)` |
   | `carpet.rule.<名>` | 供第三方工具搜索的中文名，值写 `中文名(英文名)` —— [Carpet Org Addition](https://github.com/fcsailboat/Carpet-Org-Addition) 的 `/ruleSearch` 靠它 |
   | `carpet.rule.<名>.desc` | 规则描述，`/carpet <规则名>` 显示 |

   漏了 `.name` 界面不翻译，漏了无后缀那个第三方工具搜不到。
3. 在 [`docs/zh/rules.md`](rules.md) 补一节（总览表 + 逐条说明）
4. 需要诊断手段就在 `CofDebugCommand` 加子命令，并补进 [`docs/zh/commands.md`](commands.md)；新增 mixin 记得写进 `cof.mixins.json`
5. `gradle clean build -PnoFabricRepo`，进游戏用 `/carpet list CoF` 确认名称、描述、分类都正常
6. `CHANGELOG.md` 记一笔，`gradle.properties` 提 `mod_version`，打 tag 发 Release
7. 新增**文件**时同样带上 SPDX 头：`// SPDX-License-Identifier: MIT` 与 `// Copyright (c) 2026 CoF contributors`

## 实现要点

* **零注册项**：不新增方块、物品、配方、贴图。所有功能都是对原版行为的可开关改动，卸载后存档里不会留下未定义方块
* **结冰挂在 `ServerLevel#tickChunk`**：完整复刻原版循环（`randomTickSpeed` 次迭代 × `nextInt(48) == 0`），所以节奏与原版降水结冰完全一致
* **进度用 `SavedData`**：按维度存放（`world/data/cof_cauldron_freezes.dat`），存成 `long[]` 坐标 + `int[]` 计数。只有"预热进行中"的位置才有记录，产出即删除，因此数据量天然极小
* **只读路径不创建文件**：`getIfPresent` 用于所有查询与清理，`get` 才会创建。所以从没预热过的世界不会产生数据文件
* **水瓶那条为何挂 HEAD 并取消原方法**：原版是"先消耗水瓶 → 再把手上的东西换成空玻璃瓶 → 再改方块"，等到方法返回时传入的物品**已经变成空气**，事后无法还原，也无从判断它原本是不是水瓶
* **水瓶那条为何不拦 `ItemUtils#createFilledResult`**：喝药水走同一个方法并返回空玻璃瓶，在那里拦会变成凭空刷水瓶

## 踩过的坑

| 现象 | 原因与处理 |
| --- | --- |
| `forceload` 之后锅永远不结冰 | 只加载区块不会 `tickChunk`，必须有玩家/假人在模拟距离内 |
| `/carpet cauldronFreezingChance` 后面多出一个孤立的 `]` | 规则的 `options` 为空时 Carpet 的渲染缺陷。给 int 规则补 `options` + `strict = false` 可规避 |
| `/carpet list CoF` 只显示英文规则名 | Carpet 取的是 `carpet.rule.<名>.name`，不是 `carpet.rule.<名>`；且会在后面自己补 `(规则名)` |
| 用 `/ruleSearch` 搜中文搜不到 | 那个键是 `carpet.rule.<名>`（无后缀），值要写 `中文名(英文名)` |
| 水瓶规则挂了却没生效 | 漏了"空炼药锅"这一种情况：原版会用水瓶把空锅灌成 1 级水锅 |
| 开发世界里 `setblock` 频繁报 `Could not set the block` | 区块没加载（假人掉了/没 forceload），不是命令写错 |
| 想当然地拦 `createFilledResult` 实现"喝水瓶不消耗" | 喝药水也走这个方法，会变成刷水瓶漏洞 |

## 发布

1. `gradle.properties` 提 `mod_version`
2. `gradle clean build -PnoFabricRepo`
3. commit + push，打 tag `v<版本>`（与 `mod_version` 一致）
4. GitHub Releases 上传 `dist/Carpet-CoF-Addition-<版本>.jar`（可选：一并传 `-sources.jar`）
5. 发布说明按 `CHANGELOG.md` 的分节（新内容 / 修复 / 设计与实现）抄写；**不要使用表情符号**

> **发布由维护者手动进行**：GitHub → Releases → Draft a new release → 选已有 tag（或新建 `v<版本>`）→
> 说明按上面的分节抄 `CHANGELOG.md` → 附件拖入 `dist/` 里的两个 jar → Publish。
> `release.yml` 已改为**仅手动触发**（Actions → release → Run workflow，填 tag），推 tag 不会再自动发布，
> 因此不会与手动发布互相覆盖。每次 push 与 PR 仍会自动构建一次（`build.yml`）。