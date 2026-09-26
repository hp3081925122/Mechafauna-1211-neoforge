# 机械动力装甲：模型方块定义、逐面 UV 排布与像素贴图绘制
import json
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT_PNG = os.path.join(ROOT, "src/main/resources/assets/mechafauna/textures/armor/mech_armor.png")
OUT_LAYOUT = os.path.join(ROOT, "art/mech_armor_layout.json")
TEX = 128
TEX_H = 64
PX = 1

# 调色板，取自机械犬与重装机甲贴图
C = {
    "outline": (11, 17, 27), "deep": (19, 28, 42), "dark": (37, 51, 72), "mid": (57, 76, 101),
    "steel": (89, 108, 133), "steel2": (125, 145, 170), "pale": (161, 180, 201),
    "light": (200, 214, 227), "white": (225, 236, 245), "glint": (245, 249, 253),
    "cy_dark": (7, 91, 112), "cy": (37, 206, 223), "cy_light": (185, 246, 250),
    "or_dark": (180, 74, 29), "or": (233, 109, 37), "or_light": (255, 172, 74),
}

# 材质明暗梯度：高光、亮面、中间面、暗面、最暗边
RAMPS = {
    "pale": ["glint", "white", "light", "pale", "steel2"],
    "dark": ["steel", "mid", "dark", "deep", "outline"],
    "steel": ["pale", "steel2", "steel", "mid", "dark"],
}

# 各朝向整体明暗偏移：顶面更亮，底面更暗
FACE_SHIFT = {"up": -1, "north": 0, "south": 0, "east": 0, "west": 0, "down": 1}

# 方块定义：名称、所属骨骼、起点、终点、材质；mirror 表示自动生成左侧镜像
CUBES = [
    # 头盔
    ("helm_shell", "armorHead", [-4.5, 24.3, -4.5], [4.5, 32.6, 4.5], "pale", False),
    ("helm_visor", "armorHead", [-4.2, 27.0, -5.0], [4.2, 29.4, -4.5], "dark", False),
    ("helm_jaw", "armorHead", [-3.6, 24.1, -5.0], [3.6, 26.8, -4.5], "dark", False),
    ("helm_crest", "armorHead", [-0.8, 32.6, -3.8], [0.8, 33.8, 3.4], "steel", False),
    ("helm_ear", "armorHead", [4.5, 25.8, -2.2], [5.4, 30.2, 1.8], "dark", True),
    ("helm_antenna", "armorHead", [4.75, 30.2, 0.4], [5.25, 33.4, 0.9], "steel", False),
    ("helm_neck", "armorHead", [-4.0, 23.8, 4.5], [4.0, 26.6, 5.2], "dark", False),
    # 胸甲躯干
    ("body_suit", "armorBody", [-4.4, 12.4, -2.5], [4.4, 24.4, 2.5], "dark", False),
    ("body_chest", "armorBody", [-4.1, 17.0, -3.2], [4.1, 23.9, -2.5], "pale", False),
    ("body_abs", "armorBody", [-3.4, 13.0, -2.9], [3.4, 16.6, -2.5], "steel", False),
    ("body_core", "armorBody", [-1.2, 19.0, -3.6], [1.2, 21.4, -3.2], "dark", False),
    ("body_pack", "armorBody", [-3.4, 14.6, 2.5], [3.4, 23.2, 4.4], "pale", False),
    ("body_thruster", "armorBody", [1.0, 13.0, 3.0], [3.0, 14.6, 4.6], "dark", True),
    # 胸甲手臂，右臂位于 +x，与 GeckoLib 盔甲模板一致
    ("arm_pauldron", "armorRightArm", [3.6, 20.0, -2.8], [8.8, 24.8, 2.8], "pale", True),
    ("arm_pauldron_top", "armorRightArm", [4.2, 24.8, -2.3], [8.4, 25.6, 2.3], "steel", True),
    ("arm_pauldron_fin", "armorRightArm", [8.8, 21.0, -1.8], [9.4, 24.2, 1.8], "dark", True),
    ("arm_sleeve", "armorRightArm", [3.7, 16.0, -2.4], [8.3, 20.0, 2.4], "dark", True),
    ("arm_gauntlet", "armorRightArm", [3.6, 12.0, -2.6], [8.4, 15.2, 2.6], "pale", True),
    ("arm_cuff", "armorRightArm", [3.4, 15.0, -2.7], [8.6, 16.6, 2.7], "dark", True),
    # 护腿
    ("leg_thigh", "armorRightLeg", [0.05, 5.5, -2.4], [4.3, 12.3, 2.4], "pale", True),
    ("leg_shin", "armorRightLeg", [0.1, 3.0, -2.3], [4.2, 5.5, 2.3], "dark", True),
    ("leg_knee", "armorRightLeg", [0.3, 4.5, -3.0], [3.7, 7.5, -2.4], "steel", True),
    ("leg_hip", "armorRightLeg", [4.3, 8.0, -2.0], [4.9, 12.0, 2.0], "dark", True),
    # 靴子
    ("boot_shell", "armorRightBoot", [0.05, 0.0, -2.7], [4.5, 4.2, 2.6], "pale", True),
    ("boot_toe", "armorRightBoot", [0.1, 0.0, -3.8], [4.0, 1.8, -2.7], "steel", True),
    ("boot_ankle", "armorRightBoot", [0.02, 3.4, -2.9], [4.7, 4.6, 2.8], "dark", True),
    ("boot_heel", "armorRightBoot", [0.8, 0.6, 2.6], [3.2, 2.8, 3.6], "dark", True),
]

MIRROR_BONE = {
    "armorRightArm": "armorLeftArm", "armorRightLeg": "armorLeftLeg",
    "armorRightBoot": "armorLeftBoot", "armorHead": "armorHead", "armorBody": "armorBody",
}


# 计算每个面的像素尺寸
def face_sizes(fr, to):
    dx, dy, dz = [max(1, round((to[i] - fr[i]) * PX)) for i in range(3)]
    return {"north": (dx, dy), "south": (dx, dy), "east": (dz, dy), "west": (dz, dy), "up": (dx, dz), "down": (dx, dz)}


# 简单货架式排布，面之间留 1 像素间隙防止渗色
def pack():
    items = []
    for name, bone, fr, to, mat, mirror in CUBES:
        for face, (w, h) in face_sizes(fr, to).items():
            items.append((h, w, name, face))
    items.sort(key=lambda t: (-t[0], -t[1]))
    x = y = row_h = 0
    rects = {}
    for h, w, name, face in items:
        if x + w > TEX:
            x = 0
            y += row_h + 1
            row_h = 0
        rects[(name, face)] = (x, y, w, h)
        x += w + 1
        row_h = max(row_h, h)
    if y + row_h > TEX_H:
        raise SystemExit("贴图空间不足: %d" % (y + row_h))
    return rects


# 画布写像素，越界自动忽略
def put(img, ox, oy, w, h, x, y, color):
    if 0 <= x < w and 0 <= y < h:
        img.putpixel((ox + x, oy + y), C[color] + (255,))


# 阶梯式斜切明暗，与生物贴图同风格
def base_shade(img, rect, mat, face):
    ox, oy, w, h = rect
    ramp = RAMPS[mat]
    shift = FACE_SHIFT[face]
    for y in range(h):
        for x in range(w):
            t = (x / max(1, w - 1) * 0.55 + y / max(1, h - 1) * 0.45)
            band = 1
            if t > 0.38:
                band = 2
            if t > 0.72:
                band = 3
            band = min(4, max(0, band + shift))
            if y == h - 1 or x == w - 1:
                band = min(4, band + 1)
            if y == 0 and x < w * 0.6 and h > 2:
                band = max(0, band - 1)
            put(img, ox, oy, w, h, x, y, ramp[band])


# 各方块的细节：灯带、面板缝、推进口
def details(img, rect, name, face):
    ox, oy, w, h = rect
    p = lambda x, y, c: put(img, ox, oy, w, h, x, y, c)
    if name == "helm_visor" and face == "north":
        for x in range(1, w - 1):
            p(x, h // 2, "cy")
            p(x, h // 2 - 1, "cy_dark")
        p(w // 2, h // 2, "cy_light")
        p(w // 2 - 1, h // 2, "cy_light")
    if name == "helm_shell" and face in ("east", "west", "south"):
        for y in range(2, h - 2):
            p(w // 2, y, "steel2")
    if name == "helm_ear" and face in ("east", "west"):
        for y in range(2, h - 2):
            p(w // 2, y, "cy")
    if name == "body_core" and face == "north":
        for y in range(h):
            for x in range(w):
                p(x, y, "cy" if 0 < x < w - 1 and 0 < y < h - 1 else "cy_dark")
        p(w // 2, h // 2, "cy_light")
    if name == "body_chest" and face == "north":
        for y in range(1, h - 1):
            p(w // 2, y, "steel2")
        for x in range(2, w // 2 - 2):
            p(x, h - 3, "cy")
            p(w - 1 - x, h - 3, "cy")
    if name == "body_abs" and face == "north":
        for x in range(1, w - 1):
            p(x, h // 2, "mid")
    if name == "body_pack" and face == "south":
        for y in range(2, h - 2):
            p(2, y, "cy")
            p(w - 3, y, "cy")
    if name == "body_thruster" and face == "down":
        for y in range(h):
            for x in range(w):
                edge = x in (0, w - 1) or y in (0, h - 1)
                p(x, y, "or_dark" if edge else "or")
        p(w // 2, h // 2, "or_light")
    if name == "body_thruster" and face == "south":
        for x in range(1, w - 1):
            p(x, h - 1, "or")
    if name == "arm_pauldron" and face in ("east", "west", "north"):
        for x in range(1, w - 1):
            p(x, h - 3, "cy")
    if name == "arm_cuff" and face in ("north", "east", "west", "south"):
        for x in range(w):
            p(x, h // 2, "cy")
    if name == "arm_gauntlet" and face in ("east", "west"):
        for y in range(2, h - 1):
            p(w // 2, y, "steel2")
    if name == "leg_thigh" and face in ("north", "east", "west"):
        for y in range(2, h - 2):
            p(w - 3, y, "cy")
    if name == "leg_knee" and face == "north":
        p(w // 2, h // 2, "cy_light")
        p(w // 2 - 1, h // 2, "cy")
    if name == "boot_ankle" and face in ("north", "east", "west", "south"):
        for x in range(w):
            p(x, h // 2, "cy")
    if name == "boot_heel" and face == "south":
        for x in range(1, w - 1):
            p(x, h // 2, "or")
    if name == "helm_crest" and face in ("east", "west"):
        for x in range(1, w - 1):
            p(x, 0, "cy")


def main():
    rects = pack()
    img = Image.new("RGBA", (TEX, TEX_H), (0, 0, 0, 0))
    mats = {c[0]: c[4] for c in CUBES}
    for (name, face), rect in rects.items():
        base_shade(img, rect, mats[name], face)
        details(img, rect, name, face)
    os.makedirs(os.path.dirname(OUT_PNG), exist_ok=True)
    img.save(OUT_PNG)
    layout = {
        "texture_size": TEX, "texture_height": TEX_H, "px": PX,
        "cubes": [
            {"name": n, "bone": b, "from": f, "to": t, "mirror": m,
             "uv": {face: [r[0], r[1], r[0] + r[2], r[1] + r[3]] for (cn, face), r in rects.items() if cn == n}}
            for n, b, f, t, mat, m in CUBES
        ],
        "mirror_bone": MIRROR_BONE,
    }
    with open(OUT_LAYOUT, "w", encoding="utf-8") as fh:
        json.dump(layout, fh, ensure_ascii=False, indent=1)
    print("ok", OUT_PNG)


if __name__ == "__main__":
    main()
