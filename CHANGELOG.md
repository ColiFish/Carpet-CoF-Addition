# 更新日志

本项目遵循「一版一 tag」：`gradle.properties` 里的 `mod_version` 与 git tag `vX.Y.Z` 对应，
发行产物是 `dist/Carpet CoF Addition-<版本>.jar`。

## 1.0.0

首个版本。Carpet 附属（不需要 Fabric API），共 7 条规则、1 套诊断命令。

### 功能

- **炼药锅结冰**（`waterCauldronFreezing`，默认关）：会下雪的群系 + 露天 + 方块光 < 10 时，
  水炼药锅结冰并在锅中心掉出产物。检测挂在 `ServerLevel#tickChunk`，逐位复刻原版降水循环。
- **产出档位**（`cauldronFreezingProduct`，默认 `ice`）：`ice` 每次都产出 /
  `packed_ice` 每 2 次 / `blue_ice` 每 3 次；每次产出后计数清零，需重新预热。
- **满水要求**（`cauldronFreezingRequiresFullWater`，默认开）。
- **结冰速率**（`cauldronFreezingChance`，默认 `48`，与原版同节奏；取值不受限）。
- **水桶不消耗**（`waterBucketNotConsumed`，默认关）：放水与倒进炼药锅都不消耗水桶。
- **水瓶不消耗**（`waterBottleNotConsumed`，默认关）：用水瓶给炼药锅灌水不消耗水瓶，
  也不产生空玻璃瓶（喝药水路径刻意不受影响）。
- **诊断命令**（`cofDebugCommand`，默认权限等级 2）：`/cofdebug tick | data | use | bucketcheck`。

### 设计与实现要点

- 不注册任何方块、物品、配方、贴图——卸载后存档里不会留下未定义方块。
- 预热进度用 `SavedData` 按坐标持久化，只在预热进行中才存在记录，产出/挖掉/取水即清。
- 规则名与描述带中英翻译，并提供 `carpet.rule.<名>.name` 键，
  使 `/carpet` 界面显示为「中文名(英文名)」。

### 修复与避坑（开发过程中）

- **空炼药锅**同样会被水瓶消耗（原版会把空锅灌成 1 级水锅），最初只判断了水锅，已修正。
- 水瓶那条不能拦 `ItemUtils#createFilledResult`：喝药水走同一方法并返回空玻璃瓶，
  在那里拦会变成凭空刷水瓶；改为在炼药锅交互 `useItemOn` 的 HEAD 接管。
- 该交互也不能"事后还原"：原版先消耗水瓶再换瓶，方法返回时传入的物品已变成空气。
- 规则若"建议值为空"，Carpet 会渲染出一个孤立的 `]`；给 int 规则补 `options` + `strict = false` 规避。
- 结冰需要区块 tick：只有 `forceload` 不产生检测，必须区块模拟距离内有玩家或 Carpet 假人。