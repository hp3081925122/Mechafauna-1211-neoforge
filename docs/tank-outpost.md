# 装甲维修哨站

适用环境：Minecraft 1.21.1、NeoForge 21.1.233、Mechafauna。

结构 ID：`mechafauna:tank_outpost`。模板范围为 57 × 20 × 61 格，包含地基、前门坡道和指挥室天线。

## 布局与战斗

- 中央开放维修场预置一辆机械坦克。主体两侧是维修／弹药仓库，正面有双岗楼、13 格净宽大门和宽坡道，后方是宽门车库与二层指挥室。
- 坦克出生点周围八格半径和车库至前门中央通道按实际约 3.46 × 2.74 格的碰撞尺寸留空，前门两侧设有玩家掩体。
- 前方岗楼各预置一只机械犬，与坦克一样标记持久化。和平难度仍遵循敌对生物规则。
- 左侧维修房内有一个机械犬刷怪笼，每轮一只、附近同类上限二只；玩家激活距离 10 格，生成范围 3 格。初次延迟 600 tick，之后间隔 1200–2399 tick（按 20 TPS 约 60–120 秒），允许建筑照明下刷新。
- 坦克沿用现有战斗逻辑，包括召唤蜘蛛、半血召唤机械犬和距离控制。结构不额外调整它的属性。
- 左后方三格宽外楼梯通向车库屋顶和指挥室。两侧仓库、岗楼及四个箱子都有步行可达路线。

## 战利品

三个补给箱复用 `mechafauna:chests/fortress_supplies`；指挥室军械箱复用 `mechafauna:chests/fortress_armory`。

补给箱包含机械碎片、机械犬召唤物、自爆蜘蛛投掷物或武装无人机等现有物品，并附带原版材料。军械箱保证一件机械盔甲、6–10 个机械碎片以及一份本模组特殊战利品。具体概率统一由现有战利品表维护。

## 自然生成与检查

在平原、向日葵平原、热带草原、沙漠自然生成。候选网格为 38 区块，分隔参数 14，使用独立盐值；配置距军械堡垒结构候选区域 4 区块的避让规则。采用地表高度和 `beard_thin` 地形适配，只影响新生成区域。

在空旷测试区使用以下命令；命令放置会覆盖模板范围内的方块。

```mcfunction
/place template mechafauna:tank_outpost ~5 ~ ~5
/locate structure mechafauna:tank_outpost
/place structure mechafauna:tank_outpost
```

重点实机观察坦克进出前门及车库、转炮塔与后退时是否碰墙，左后楼梯到指挥室是否顺畅，以及丘陵边缘生成时的地基衔接。方块数据预览中的青色线框表示坦克出生范围，预览不是游戏截图。

## 文件

- `src/main/resources/data/mechafauna/structure/tank_outpost.nbt`
- `src/main/resources/data/mechafauna/worldgen/structure/tank_outpost.json`
- `src/main/resources/data/mechafauna/worldgen/template_pool/tank_outpost.json`
- `src/main/resources/data/mechafauna/worldgen/structure_set/tank_outpost.json`
- `src/main/resources/data/mechafauna/tags/worldgen/biome/has_structure/tank_outpost.json`
- `src/main/java/com/hp/mechafauna/gametest/TankOutpostGameTests.java`
- `art/tank-outpost/overview.png`、`cutaway.png`、`floorplan.png`

构造脚本为 `D:\AI临时文件\build_tank_outpost.py`；布局检查清单为 `D:\AI临时文件\tank_outpost_qa\report.json`；游戏测试临时目录为 `D:\AI临时文件\tank-outpost-gametest`。构造脚本重建时会覆盖模板及预览，手工修改需要同步回脚本。

```powershell
python -X utf8 'D:\AI临时文件\build_tank_outpost.py'
$env:JAVA_HOME = 'C:\Users\30819\.jdks\ms-21.0.11'
.\gradlew.bat -I 'D:\AI临时文件\tank-outpost-gametest.init.gradle' runGameTestServer jar
```
