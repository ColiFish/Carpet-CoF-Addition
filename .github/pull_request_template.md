## 这个 PR 做了什么

<!-- 一句话说明动机与改动；关联的 issue 写 "Closes #123" -->

## 自查清单

- [ ] 本地构建通过：`gradle clean build -PnoFabricRepo`（或用 `./gradlew clean build -PnoFabricRepo`）
- [ ] 游戏内实测过，并在下面写清测试方式（生存/创造、玩家/假人、用了哪条 `/cofdebug`）
- [ ] 中英文档同步更新（`docs/zh/` 与 `docs/en/` 是两份，容易只改一份）；构建、测试与新增规则的步骤见 `docs/zh/dev-notes.md`
- [ ] 新增规则时：三个翻译键齐全（`carpet.rule.<名>.name` / `carpet.rule.<名>` / `carpet.rule.<名>.desc`），`docs/zh/rules.md` 与 `docs/en/rules.md` 的总览表已加行
- [ ] 新增文件时：已带 SPDX 头（`// SPDX-License-Identifier: MIT` 与版权行）
- [ ] `CHANGELOG.md` 已记录，且未使用表情符号
- [ ] 行为有变化时，已提升 `gradle.properties` 里的 `mod_version`

## 测试记录

<!-- 贴关键命令与输出，例如 /cofdebug data <坐标> 的结果、结冰前后的方块与水位 -->

```
（在此粘贴）
```
