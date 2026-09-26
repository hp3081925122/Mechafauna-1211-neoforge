# 飞碟母舰死亡动画生成脚本
import json
import math
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ANIM_FILE = ROOT / "src" / "main" / "resources" / "assets" / "mechafauna" / "animations" / "flying_saucer_boss.animation.json"

# 死亡动画时长 100 tick = 5.0 秒
DEATH_LENGTH = 5.0
FPS = 20.0

def ease_in_cubic(t):
    return t * t * t

def ease_out_cubic(t):
    return 1.0 - (1.0 - t) ** 3

# 母舰在 5 秒内下坠并解体
bones = {}

# hull 整体下坠，不倾斜
hull_pos = {}
for tick in range(0, int(DEATH_LENGTH * FPS) + 1):
    t = tick / (DEATH_LENGTH * FPS)
    time = tick / FPS
    # 前 2 秒轻微震荡,后 3 秒加速下坠
    if t < 0.4:
        shake = math.sin(t * 40.0) * 0.3
        hull_pos[f"{time:.2f}"] = [0, shake, 0]
    else:
        fall_progress = (t - 0.4) / 0.6
        fall = -15.0 * ease_in_cubic(fall_progress)
        hull_pos[f"{time:.2f}"] = [0, fall, 0]

bones["hull"] = {"position": hull_pos}

# 上方传感器盖失效塌陷
cap_rot = {}
for tick in range(0, int(DEATH_LENGTH * FPS) + 1):
    t = tick / (DEATH_LENGTH * FPS)
    time = tick / FPS
    if t < 0.3:
        cap_rot[f"{time:.2f}"] = [0, 0, 0]
    else:
        droop = (t - 0.3) / 0.7
        cap_rot[f"{time:.2f}"] = [0, 0, -35.0 * ease_out_cubic(droop)]
bones["upper_sensor_cap"] = {"rotation": cap_rot}

# 8 个盖板分别往外翻开
for i in range(8):
    facet_rot = {}
    phase_offset = i * 0.05
    for tick in range(0, int(DEATH_LENGTH * FPS) + 1):
        t = tick / (DEATH_LENGTH * FPS)
        time = tick / FPS
        start_t = 0.25 + phase_offset
        if t < start_t:
            facet_rot[f"{time:.2f}"] = [0, 0, 0]
        else:
            progress = (t - start_t) / (1.0 - start_t)
            angle = 60.0 * ease_out_cubic(progress)
            facet_rot[f"{time:.2f}"] = [angle, 0, 0]
    bones[f"cap_facet_{i}"] = {"rotation": facet_rot}

# 能量核心环加速旋转后熄灭(靠特效表现熄灭,这里只做旋转)
core_rings_rot = {}
for tick in range(0, int(DEATH_LENGTH * FPS) + 1):
    t = tick / (DEATH_LENGTH * FPS)
    time = tick / FPS
    # 前 1.5 秒加速,后面保持
    if t < 0.3:
        spin_speed = t / 0.3
        angle = spin_speed * 360.0 * t * 3.0
    else:
        angle = 360.0 * 3.0 * ((t - 0.3) + 0.3)
    core_rings_rot[f"{time:.2f}"] = [0, angle % 360, 0]
bones["core_rings"] = {"rotation": core_rings_rot}

# 构造最终 JSON
anim_data = json.loads(ANIM_FILE.read_text(encoding="utf-8"))
anim_data["animations"]["animation.flying_saucer_boss.death"] = {
    "loop": False,
    "animation_length": DEATH_LENGTH,
    "bones": bones
}

ANIM_FILE.write_text(json.dumps(anim_data, ensure_ascii=False, indent=2), encoding="utf-8")
print(f"已生成死亡动画: {ANIM_FILE}")
print(f"动画时长: {DEATH_LENGTH} 秒,骨骼数: {len(bones)}")
