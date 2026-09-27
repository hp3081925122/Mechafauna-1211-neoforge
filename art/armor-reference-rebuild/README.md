# 机械盔甲图标重建

参考：`../armor-icons-sampled/preview.png`。外形依据为用户接受的四件 16×16 图标；头盔、胸甲、护腿与靴子从空 Blockbench 工程重建。侧面和背面为适配穿戴而补全的结构。

正式工程为 `../mech_armor.bbmodel`，正式游戏资源为 `../../src/main/resources/assets/mechafauna/geo/armor/mech_armor.geo.json` 与 `../../src/main/resources/assets/mechafauna/textures/armor/mech_armor.png`。

`helmet`、`chestplate`、`leggings`、`boots` 各包含独立可编辑工程、导出几何、贴图与实际 Blockbench 预览。`final` 包含整套工程副本和 256×128 图集；共 73 个方块、14 个骨骼，27 对部件使用几何和 UV 镜像。图集按每模型单位约 2 个纹理像素绘制。

文件检查覆盖唯一标识、骨骼父级、UV 边界、透明通道，以及独立分件与整套模型几何和逐面纹理的一致性。结果见 `final/asset-validation.json`。

重新打开保存后的完整工程，与正式导出几何核对一致，完成 48 帧环绕与俯仰记录。有效记录在 `final/verified-rotation`，汇总图为 `final/rotation-contact-sheet.png`，播放文件为 `final/rotation.gif`。`final/inspection` 是首次导入中断产生的无效空白帧，不属于验收记录。

`processResources --offline` 已通过。尚未进行游戏内实际穿戴、行走、蹲下和持物姿态检查。

重建脚本在 `D:\AI临时文件\rebuild_mech_armor.py`，模型构建及截图脚本也在 `D:\AI临时文件`。项目原有 `../mech_armor_texture.py` 属于上一版，不能用于生成此版图集。
