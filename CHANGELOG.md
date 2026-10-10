# 更新日志

本文件与 GitHub Release 说明一律**不使用表情符号**。

本项目遵循「一版一 tag」：`gradle.properties` 里的 `mod_version` 与 git tag `vX.Y.Z` 对应，发行产物是 `dist/Carpet-CoF-Addition-<版本>.jar`。

## 1.0.0

首个版本。Carpet 附属，共 7 条规则、1 套诊断命令。

### 新内容

**规则**

- **炼药锅结冰**（`waterCauldronFreezing`，默认关）：会下雪的群系 + 露天 + 方块光 < 10 时，水炼药锅结冰并在锅中心掉出产物。检测挂在 `ServerLevel#tickChunk`，逐位复刻原版降水循环
- **炼药锅结冰产物**（`cauldronFreezingProduct`，默认 `ice`）：`ice` 每次都产出 / `packed_ice` 每 2 次 / `blue_ice` 每 3 次；每次产出后计数清零，需重新预热
- **炼药锅结冰需要满水**（`cauldronFreezingRequiresFullWater`，默认开）
- **炼药锅结冰速率**（`cauldronFreezingChance`，默认 `48`，与原版同节奏；取值不受限）
- **水桶不消耗**（`waterBucketNotConsumed`，默认关）：放水与倒进炼药锅都不消耗水桶
- **水瓶不消耗**（`waterBottleNotConsumed`，默认关）：用水瓶给炼药锅灌水不消耗水瓶，也不产生空玻璃瓶（喝药水路径刻意不受影响）

**命令**

- **CoF调试工具**（`cofDebugCommand`，默认权限等级 2）：`/cofdebug tick | data | use | bucketcheck`

**文档**

- README 面向使用者重写，正文拆分为[规则文档](docs/zh/rules.md)、[命令文档](docs/zh/commands.md)、[开发文档](docs/zh/development.md)三份
- 全部文档提供**英文版**：[README_en.md](README_en.md)、[docs/en/rules.md](docs/en/rules.md)、[docs/en/commands.md](docs/en/commands.md)、[docs/en/development.md](docs/en/development.md)

### 修复

- **空炼药锅同样会被水瓶消耗**：原版允许用水瓶把空锅灌成 1 级水锅，这是最初漏掉的一种情形
- 水瓶规则改为在炼药锅交互 `useItemOn` 的 HEAD 接管：原版先消耗水瓶再换瓶，方法返回时传入的物品已变成空气，无法事后还原
- 结冰速率为 int 且 `options` 为空时，`/carpet` 会渲染出一个孤立的 `]`；补 `options` + `strict = false` 规避

### 设计与实现

- 不注册任何方块、物品、配方、贴图 —— 卸载后存档里不会留下未定义方块
- 预热进度用 `SavedData` 按坐标持久化，只在预热进行中才存在记录，产出/挖掉/取水即清
- 提供 `carpet.rule.<名>.name` 键，使 `/carpet` 界面显示为「中文名(英文名)」
- 以 **GPL-3.0-only** 发布（见 LICENSE）
