# 更新日志

本项目遵循「一版一 tag」：`gradle.properties` 里的 `mod_version` 与 git tag `vX.Y.Z` 对应，发行产物是 `dist/Carpet-CoF-Addition-<版本>.jar`。

## 1.0.2

继续修正 `/cofdebug` 的细节，并把文档与实际行为对齐。

### 修复

- `/cofdebug data <坐标> <值>` 现在把计数写在与结冰逻辑相同的那口锅上（该列地表 / 地表下一格）：
  此前写在输入坐标上，锅被雪层或方块盖住时会"写入成功却读不到"，推进度也不掉产物
- 该子命令在目标坐标与输入不一致、以及总开关关闭时会各给一行提示，不再默默写入
- 权限判定的 javadoc 与实现对齐（此前仍写着"解析失败退回 2 级"，与 fail-closed 实现矛盾）

### 文档

- 规则与命令文档补上 1.0.1 的行为变化：`tick` 的目标行与三种结果、总开关限制、
  `bucketcheck` 创造模式跳过、`data 0` 的只读语义、`cofDebugCommand` 的失败即关闭
- 只读 `data` 的目标搜索优先级、`累计` 是步进后的值、组合用法示例一并写清
- 英文语言文件补齐 7 条规则的 `carpet.rule.<名>.name`，英文客户端也能显示「英文名(规则名)」

## 1.0.1

修复 `/cofdebug` 诊断命令的一批行为问题，并加固构建流程。

### 修复

- `/cofdebug tick` 现在遵守总开关 `waterCauldronFreezing`：关闭时拒绝执行，不再把锅抽干、掉出产物
- `/cofdebug tick` 如实区分"仅累计"与"真产出"，并新增 `目标` 行显示实际会被处理的那口锅
- `/cofdebug data` 与 `tick` 共用同一套目标定位：只读报告与实际结冰目标一致，写入也落在同一口锅上
- `/cofdebug bucketcheck` 在创造模式下直接跳过：该路径会把测试物品放进玩家背包，数字也不代表生存语义
- `/cofdebug data <坐标> 0` 改走只读路径，不再凭空建出空的存档数据文件
- `/cofdebug` 的权限判定改为失败即关闭：非法取值一律视为关闭，负数拒绝，权限等级钳制到 0~4

### 设计与实现

- 结冰检查抽出 `FreezeResult` 返回值与共用的 `findCauldron()`，让诊断与真实逻辑不再各猜一遍
- 文档措辞修正：检测频率是"与原版降水循环同结构、同 1/48 概率门"，而不是逐位一致
- 构建流程加固：工作流补 `permissions` 与 `concurrency`；Carpet 下载加 `--fail`、重试、大小检查与 sha512 校验；发布前校验 tag 与 `mod_version` 一致
- `.gitattributes` 标记 PowerShell 脚本不参与语言统计
## 1.0.0

首个版本。Carpet 附属，共 7 条规则、1 套诊断命令。

### 新内容

**规则**

- **炼药锅结冰**（`waterCauldronFreezing`，默认关）：会下雪的群系 + 露天 + 方块光 < 10 时，水炼药锅结冰并在锅中心掉出产物。检测挂在 `ServerLevel#tickChunk`，与原版降水循环同结构、同 1/48 概率门
- **炼药锅结冰产物**（`cauldronFreezingProduct`，默认 `ice`）：`ice` 每次都产出 / `packed_ice` 每 2 次 / `blue_ice` 每 3 次；每次产出后计数清零，需重新预热
- **炼药锅结冰需要满水**（`cauldronFreezingRequiresFullWater`，默认开）
- **炼药锅结冰速率**（`cauldronFreezingChance`，默认 `48`，与原版同节奏；取值不受限）
- **水桶不消耗**（`waterBucketNotConsumed`，默认关）：放水与倒进炼药锅都不消耗水桶
- **水瓶不消耗**（`waterBottleNotConsumed`，默认关）：用水瓶给炼药锅灌水不消耗水瓶，也不产生空玻璃瓶（喝药水路径刻意不受影响）
- **CoF调试工具**（`cofDebugCommand`，默认 `2`）：是否启用 `/cofdebug` 诊断命令，`0`~`4` 为要求的最低权限等级（改完立刻生效）

**命令**

- `/cofdebug`：`tick <坐标>` 手动步进一次结冰检查 · `data <坐标>` 只读复述全部条件与进度 · `data <坐标> <值>` 改写累计次数 · `use <坐标> [玩家]` 对目标方块真实右键一次并播报前后变化 · `bucketcheck [玩家]` 调用原版判定验证水桶规则

**文档**

- README 面向使用者重写，正文拆分为[规则文档](docs/zh/rules.md)与[命令文档](docs/zh/commands.md)两份
- 全部文档提供**英文版**：[README_en.md](README_en.md)、[docs/en/rules.md](docs/en/rules.md)、[docs/en/commands.md](docs/en/commands.md)

### 修复

- **空炼药锅同样会被水瓶消耗**：原版允许用水瓶把空锅灌成 1 级水锅，这是最初漏掉的一种情形
- 水瓶规则改为在炼药锅交互 `useItemOn` 的 HEAD 接管：原版先消耗水瓶再换瓶，方法返回时传入的物品已变成空气，无法事后还原
- 结冰速率为 int 且 `options` 为空时，`/carpet` 会渲染出一个孤立的 `]`；补 `options` + `strict = false` 规避

### 设计与实现

- 不注册任何方块、物品、配方、贴图 —— 卸载后存档里不会留下未定义方块
- 预热进度用 `SavedData` 按坐标持久化，只在预热进行中才存在记录，产出/挖掉/取水即清
- 提供 `carpet.rule.<名>.name` 键，使 `/carpet` 界面显示为「中文名(英文名)」
- 以 **MIT** 发布（见 LICENSE）
