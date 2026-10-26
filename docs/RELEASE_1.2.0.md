# Mechafauna 1.2.0 发布记录

- 日期：2026-10-26。
- 版本：`26.1.2-neoforge-1.1.0` → `26.1.2-neoforge-1.2.0`。
- 只递增版本并构建发布；不迁移 1211 后续 showcase 改动。
- 与上一正式包逐项比较，只有 `META-INF/neoforge.mods.toml` 改变，全部 Java 类与已验证的 1.1.0 一致。
- `gradlew.bat build --offline` 通过，武器瞄准检查 6,046 项通过；沿用本次已完成的服务端及 Render Preview 验证。
- 上传文件：`26.1.2-neoforge-Mechafauna-1.2.0.jar`。
- SHA-256：`881ed5096ea4b23f5a600f51e1e7a19d1924300f7d91b75aad7570f2928e8b06`。
- CurseForge 项目：`1713923`，`mechafauna`。
- 文件 ID：`9330932`，地址：https://www.curseforge.com/minecraft/mc-mods/mechafauna/files/9330932。
- 上传接口返回 HTTP 200 和文件 ID；已设置 Release、审核后自动发布、中英文更新说明、GeckoLib 必需依赖。
- 上传后公开详情页暂返回 HTTP 404，只确认上传已受理，尚未确认审核通过或公开可下载；不重复上传。
- 标签：26.1.2、NeoForge、Java 25、Client、Server；标签通过官方接口核对，上传使用完整版本名称。
- 上传结果、发布元数据和中英文更新说明：`D:\AI临时文件\mechafauna-sync-20261026\cf-release-1.2.0`。

## 中文更新摘要

飞碟三种新技能、残骸暴走及默认 4% 的非玩家额外百分比伤害；重装机甲轨道齐射、冲撞和核心过载；坦克轨道炮、迫击炮和能量护盾。包含战斗 AI、召唤归属、动画、特效、贴图、骨骼瞄准及透明渲染管线修正。

## English summary

Includes the mothership's three new skills, wreckage rage, and configurable 4% bonus damage against non-player targets; Heavy Mech barrage, charge, and overload; and tank railgun, mortar, and shield phases. Also includes combat AI, summon ownership, animation, VFX, textures, bone aiming, and transparent rendering pipeline fixes.
