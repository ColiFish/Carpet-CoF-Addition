# Carpet CoF Addition

**中文** | [English](README_en.md)

## 依赖

| 依赖 | 具体版本 | 下载 |
| --- | --- | --- |
| Minecraft | **1.21.4** | — |
| Fabric Loader | **≥ 0.16.9** | [客户端](https://fabricmc.net/use/installer/) / [服务端](https://fabricmc.net/use/server/) |
| [Carpet](https://github.com/gnembon/fabric-carpet) | **1.4.161+v241203**（MC 1.21.4 版） | [Modrinth](https://modrinth.com/mod/carpet/versions?g=1.21.4) · [GitHub Releases](https://github.com/gnembon/fabric-carpet/releases) |
| Java | **21** | [Oracle 官方下载（Java 21）](https://www.oracle.com/java/technologies/downloads/#java21) |

## 安装

1. **服务端**：先装 [Fabric 服务端](https://fabricmc.net/use/server/)，再把 **Carpet** 与本模组的 jar 一起放进服务端的 `mods/` 目录
2. **单人 / 局域网**：客户端也要装 Fabric + Carpet + 本模组——本模组把 Carpet 声明为硬依赖，客户端缺它会直接启动失败
3. 只下载 `Carpet-CoF-Addition-<版本>.jar`，**不要**下载 `-sources.jar`（那只是源码包，放进 `mods/` 会被当成模组而启动失败）

## 下载

本模组从 [Releases](../../releases) 下载：请下 `Carpet-CoF-Addition-<版本>.jar`（**不要**下 `-sources.jar`，那只是源码包，放进 `mods/` 会被当成模组而启动失败）。

Java 21 只有**自建服务端**或**手动指定 Java 启动**时才需要自己装：官方启动器会自带运行时，HMCL / PCL 这类启动器也能自动下载。

## 文档

| 文档 | 内容 |
| --- | --- |
| [规则文档](docs/zh/rules.md) | 7 条规则的逐条说明：默认值、可选值、生效条件、示例、注意事项 |
| [命令文档](docs/zh/commands.md) | `/cofdebug` 诊断命令的语法、参数、输出样例与组合用法 |

英文版：[README_en.md](README_en.md) · [docs/en/](docs/en/)（同两份文档的英文版）

## 常见问题

**开了规则锅还是不结冰？**

四个条件必须同时满足：会下雪的群系、锅在该列地表且头顶露天、该位置方块光 < 10、锅里有足够的水（默认要求满水）。另外**必须有玩家或 Carpet 假人在区块模拟距离内** —— 只加载区块（`forceload`）不会触发检测。用 `/cofdebug data <坐标>` 可以直接看到哪一条不满足。

**为什么默认是关的？**

Carpet 系模组的惯例：所有功能显式开启，避免影响既有的存档与服务器。

**会虚空刷冰吗？**

不会。水是**真的被消耗**的：产出的那一次锅会被清空，和原版水源结冰一样。

**石岸这种群系为什么不结冰？**

结冰跟随原版的**雪线**：判定用 `Biome#getPrecipitationAt(坐标, 海平面)`，它会按高度修正温度。雪线以下原版判为下雨，所以那里不会结冰；锅上下挪动、跨过雪线就会改变结果。

**创造模式测不出水桶/水瓶规则？**

创造模式本来就不消耗物品，所以那两个开关看不出差别。请在生存模式下测试。

**存档里会留下什么？**

一个数据文件 `world/data/cof_cauldron_freezes.dat`（每个维度一份），而且**只记录"正在预热中"的位置** —— 产出、挖掉、取水都会立刻清除记录。从没预热过任何锅的世界不会产生这个文件。



## 警告

本mod为vibe coding产物，请谨慎使用
