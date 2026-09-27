# 重装军械堡垒

适用环境：Minecraft 1.21.1、NeoForge 21.1.233、Mechafauna。

## 内容

- 模板范围：57 × 23 × 45 格（X/Y/Z，包含地基、屋顶设备和东侧外部警戒台）。
- 一层：装甲入口、前厅、两翼维修／物资房、中央机甲大厅、后方通道。
- 二层：两座三格宽楼梯、环形回廊、维修侧室、控制室与主战利品箱。
- 二层左右维修翼各固定放置一只喷火爬行者；这是结构内预置的持久生物，不是刷怪笼。
- 东侧外墙开门连接露天警戒台，警戒台位于主体建筑外，设一个棱光境刷怪笼。
- 中央大厅预置一台 `mechafauna:heavy_mech`，正常 AI，标记持久化，不因距离自然消失；和平难度仍遵循敌对生物规则。
- 不包含坦克，也不使用坦克刷怪笼。
- 五个原版刷怪笼：主体内两个机械犬、两个自爆蜘蛛；外侧警戒台一个棱光境。前四个首次延迟 100 tick，之后每 500–799 tick 尝试生成一只，激活距离 12 格，生成范围 3 格，附近同类上限 3。附近上限不是整个建筑的全局上限。
- 棱光境刷怪笼同时限制天空光照不高于 7，且等待 3600–7199 tick 才再次尝试；激活距离 14 格，生成范围 3 格，附近棱光境上限 1。它主要在夜晚或足够阴暗的天气低频刷新，不是每次进入结构都保证出现。
- 五个刷怪笼均附带自定义光照条件，以便主体内刷怪笼在建筑照明下工作；和平难度不出怪。
- 五个战利品箱：四个补给箱、一个控制室军械箱。每个补给箱都保证提供机械碎片或一件本模组工具：机械犬召唤物、自爆机械蜘蛛或武装无人机；并额外随机提供 3–5 轮原版基础材料。
- 军械箱保证提供一件机械盔甲、6–10 个机械碎片和一份军械奖励。军械奖励会从棱光镜核心、武装无人机、机械犬召唤物、自爆机械蜘蛛中随机选择；另有 3–5 轮材料。各战利品箱都使用独立战利品表，开启后不会重复刷新。
- 建筑全用原版方块，无新增方块注册或逐 tick 建筑扫描。

机甲及刷怪笼产物沿用模组现有战斗逻辑。当前公共爆炸实现使用 `ExplosionInteraction.NONE`，不会炸毁方块；没有为这座结构额外修改全局战斗行为。

## 自然生成

结构 ID：`mechafauna:heavy_mech_fortress`。

群系：平原、向日葵平原、热带草原。按 40 区块网格设置候选位置、分隔参数 16；这不是固定每 640 格必有一座。使用地表高度和 `beard_thin` 地形适配。只在新生成的区块出现，不向已经生成的区块补建。

大型结构在起伏地形上的地基与地形衔接仍需实景检验，不能把群系限制当作整片地面绝对平坦的保证。

## 测试命令

先在空旷测试区放置；命令放置会覆盖占地内方块。

```mcfunction
/place template mechafauna:heavy_mech_fortress ~5 ~ ~5
/place structure mechafauna:heavy_mech_fortress
/locate structure mechafauna:heavy_mech_fortress
```

`place template` 的坐标是模板角点，室内地板位于模板 Y+3；命令定位结构时使用世界生成入口，不等同于固定角点摆放。

重点体验：三格高以上门洞的进出、两座楼梯及二层回廊、箱盖是否畅通、机甲绕柱移动和室内射击、刷怪笼停留压力，以及自然地形衔接。机甲血量及能力没有为结构单独加强。

## 文件

- `src/main/resources/data/mechafauna/structure/heavy_mech_fortress.nbt`
- `src/main/resources/data/mechafauna/worldgen/structure/heavy_mech_fortress.json`
- `src/main/resources/data/mechafauna/worldgen/template_pool/heavy_mech_fortress.json`
- `src/main/resources/data/mechafauna/worldgen/structure_set/heavy_mech_fortress.json`
- `src/main/resources/data/mechafauna/tags/worldgen/biome/has_structure/heavy_mech_fortress.json`
- `src/main/resources/data/mechafauna/loot_table/chests/fortress_supplies.json`
- `src/main/resources/data/mechafauna/loot_table/chests/fortress_armory.json`
- `src/main/java/com/hp/mechafauna/gametest/FortressGameTests.java`

按用户要求，构造脚本与测试临时目录均在 `D:\AI临时文件`，不打入模组：

- `D:\AI临时文件\build_mech_fortress.py`：根据确定性的方块布局生成 NBT，并做连通检查。
- `D:\AI临时文件\mech_fortress_qa`：实际方块平面、剖面与清单；不是游戏截图。
- `D:\AI临时文件\mech-fortress-gametest.init.gradle`：把游戏测试工作目录切到临时目录，并启用本模组测试。

重建结构会替换 NBT；手工修改 NBT 前应先保留副本，并同步到构造脚本，避免下次重建覆盖手工变化。

```powershell
$env:JAVA_HOME = 'C:\Users\30819\.jdks\ms-21.0.11'
python 'D:\AI临时文件\build_mech_fortress.py'
.\gradlew.bat -I 'D:\AI临时文件\mech-fortress-gametest.init.gradle' runGameTestServer
.\gradlew.bat build
```
