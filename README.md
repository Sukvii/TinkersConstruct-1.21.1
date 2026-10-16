# Tinkers' Construct 1.21.1 移植版

本项目是 Tinkers' Construct 面向 **Minecraft 1.21.1 + NeoForge** 的社区移植版，对照官方 Mantle `v1.11.117` 与 Tinkers' Construct `v3.12.0.220`，同时保留本分支的 1.21.1 平台适配和本地修复。

## 项目信息

- Minecraft：`1.21.1`
- NeoForge：`21.1.241`
- Java：`21`
- Tinkers' Construct 移植版本：`3.12.6`
- 必需依赖：Mantle `1.12.5`
- 可选 JEI 编译版本：`19.51.0.416`
- 状态：开发版，不保证与旧存档 / 旧版本互通

当前 U8 运行矩阵保留 17 项，其中 R18 / R19 为 `pass`，其余 15 项为 `not_run`。这份开发版不宣称全部客户端和专服场景、旧样本或兼容组合已经验收。

## 项目目录

```text
TinkersConstruct-1.21.1/
Mantle-1.21.1/
```

Mantle 与 Tinkers' Construct 是两个独立模组，运行时需要同时安装匹配版本。构建产物分别位于：

```text
TinkersConstruct-1.21.1/build/libs/TinkersConstruct-*.jar
Mantle-1.21.1/build/libs/Mantle-*.jar
```

## 构建

需要 JDK 21。Tinkers' Construct 项目会将相邻的 Mantle 目录作为源码子项目构建：

```bash
./gradlew build
```

## 参考

- 上游项目：[Tinkers' Construct](https://github.com/SlimeKnights/TinkersConstruct)、[Mantle](https://github.com/SlimeKnights/Mantle)
- 上游文档：https://slimeknights.github.io/docs/
- 游戏内容概览：https://slimeknights.github.io/docs/gameplay/tinkers-construct-3/
- 社区支持：https://discord.com/invite/njGrvuh
