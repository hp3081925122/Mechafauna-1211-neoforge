# 飞碟母舰死亡动画生成脚本
import json
import math
import os

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src", "main", "resources", "assets", "mechafauna", "animations", "flying_saucer_boss_death.animation.json")

LENGTH = 5.0
STEPS = 100

def ease_in_out(t):
    # 缓动函数
    if t < 0.5:
        return 2 * t * t
    else:
        return 1 - 2 * (1 - t) * (1 - t)

frames = {}

# hull 整体翻滚下坠
for i in range(STEPS + 1):
    t = i / STEPS
    time = t * LENGTH

    # 前2秒上下晃动,然后下坠
    if t < 0.4:
        wobble = 0.3 * math.sin(t * 20)
    else:
        wobble = -15 * ease_in_out((t - 0.4) / 0.6)

    # 翻滚加速
    roll = 25 * t * t * t

    frames[f"{time:.2f}"] = [0, wobble, 0] if i <= 40 else [0, wobble, roll]

hull_rotation = {f"{time:.2f}": [0, 0, 25 * (time / LENGTH) ** 3] for time in [i * LENGTH / STEPS for i in range(STEPS + 1)]}
hull_position = {}
for i in range(STEPS + 1):
    t = i / STEPS
    time = t * LENGTH
    if t < 0.4:
        y = 0.3 * math.sin(t * math.pi * 10)
    else:
        y = -15 * ease_in_out((t - 0.4) / 0.6)
    hull_position[f"{time:.2f}"] = [0, y, 0]

# 传感器帽破损脱落
cap_rotation = {}
for i in range(STEPS + 1):
    t = i / STEPS
    time = t * LENGTH
    if t < 0.3:
        cap_rotation[f"{time:.2f}"] = [0, 0, 0]
    else:
        progress = (t - 0.3) / 0.7
        cap_rotation[f"{time:.2f}"] = [0, 0, -35 * ease_in_out(progress)]

# 能量核心疯狂旋转
core_rotation = {}
for i in range(STEPS + 1):
    t = i / STEPS
    time = t * LENGTH
    core_rotation[f"{time:.2f}"] = [0, 720 * t, 0]

# 武器挂架下垂
def weapon_droop(mount_name, side):
    rotation = {}
    for i in range(STEPS + 1):
        t = i / STEPS
        time = t * LENGTH
        if t < 0.4:
            rotation[f"{time:.2f}"] = [0, 0, 0]
        else:
            progress = (t - 0.4) / 0.6
            pitch = -60 * ease_in_out(progress)
            roll = side * 30 * ease_in_out(progress)
            rotation[f"{time:.2f}"] = [pitch, 0, roll]
    return rotation

# 装甲板脱离
def armor_jitter(index):
    rotation = {}
    for i in range(STEPS + 1):
        t = i / STEPS
        time = t * LENGTH
        if t < 0.25 + index * 0.05:
            rotation[f"{time:.2f}"] = [0, 0, 0]
        else:
            phase = (t - 0.25 - index * 0.05) * 30
            jitter_x = 60 * math.sin(phase + index) * min(1, (t - 0.25 - index * 0.05) * 3)
            jitter_z = 45 * math.cos(phase * 1.3 + index) * min(1, (t - 0.25 - index * 0.05) * 3)
            rotation[f"{time:.2f}"] = [jitter_x, 0, jitter_z]
    return rotation

animation = {
    "format_version": "1.8.0",
    "animations": {
        "animation.flying_saucer_boss.death": {
            "loop": False,
            "animation_length": LENGTH,
            "bones": {
                "hull": {
                    "rotation": hull_rotation,
                    "position": hull_position
                },
                "upper_sensor_cap": {
                    "rotation": cap_rotation
                },
                "core_mount": {
                    "rotation": core_rotation
                },
                "starboard_gatling_mount": {
                    "rotation": weapon_droop("starboard_gatling_mount", -1)
                },
                "port_gatling_mount": {
                    "rotation": weapon_droop("port_gatling_mount", 1)
                },
                "fore_missile_mount": {
                    "rotation": weapon_droop("fore_missile_mount", 0)
                },
                "aft_missile_mount": {
                    "rotation": weapon_droop("aft_missile_mount", 0)
                }
            }
        }
    }
}

# 添加16块装甲板的抖动
for i in range(16):
    animation["animations"]["animation.flying_saucer_boss.death"]["bones"][f"armor_sector_{i:02d}"] = {
        "rotation": armor_jitter(i)
    }

os.makedirs(os.path.dirname(OUT), exist_ok=True)
with open(OUT, "w", encoding="utf-8") as f:
    json.dump(animation, f, indent=2, ensure_ascii=False)

print(f"Generated {OUT}")
print(f"Animation length: {LENGTH}s, {STEPS + 1} keyframes per bone")
