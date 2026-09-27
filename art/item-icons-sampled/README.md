# 物品图标采样重制

本目录包含五个普通物品与八个刷怪蛋的新图标。四件已认可的盔甲图标未修改。

- `references/`：逐项生成的透明底参考图。
- `icons/`：最终 16×16 RGBA 游戏纹理，透明度仅为 0 或 255。
- `preview.png`：最近邻放大的总览。
- `sampling-report.json`：裁切范围、采样尺寸与特征点采样记录。
- `validation.json`：尺寸、透明度、模型引用及旧盔甲未修改的校验结果。

处理使用透明区域裁切和最近邻采样，不使用平滑或锐化。无人机青色镜头从原参考图额外选取一个特征点，防止粗网格采样遗漏。无人机和重装机甲蛋使用 15 像素主体跨度，其他图标使用 14 像素主体跨度，再放入 16×16 画布。

最终纹理已复制到 `src/main/resources/assets/mechafauna/textures/item/`。八个刷怪蛋使用独立图标并以白色倍率保留纹理原色。棱光镜核心的背包图标使用采样纹理，其他显示场景继续使用完整原立体模型 `prism_core_in_hand.json`。

原始受影响文件备份：`D:\AI临时文件\mech-item-icons-before-20260927`。
采样及验证脚本：`D:\AI临时文件\sample_all_mech_items.py`、`D:\AI临时文件\validate_mech_item_icons.py`。
