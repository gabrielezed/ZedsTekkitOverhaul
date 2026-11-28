"""Face-wise pixel remapping from owned Thief and Saboteur UV layouts."""

from collections import Counter
from pathlib import Path

from PIL import Image, ImageDraw

ART = Path(__file__).resolve().parents[1]
TEXTURES = ART.parent / "src/minecraft/zed/textures"
SOURCES = ART / "source/abyssal"
NEAREST = Image.Resampling.NEAREST
THIEF_HEAD = (38, 0, 6, 6, 5)
SABOTEUR_HEAD = (20, 0, 6, 5, 5)
THIEF_TORSO = (16, 0, 6, 9, 5)
SABOTEUR_TORSO = (0, 0, 5, 8, 4)
ARM = (0, 16, 2, 8, 2)
HAND = (8, 16, 3, 9, 3)
LEG = (0, 32, 2, 9, 3)


def faces(u, v, w, h, d):
    return [
        (u, v+d, d, h), (u+d, v+d, w, h), (u+d+w, v+d, d, h),
        (u+2*d+w, v+d, w, h), (u+d, v, w, d), (u+d+w, v, w, d),
    ]


def crop(image, bounds):
    x, y, w, h = bounds
    return image.crop((x, y, x+w, y+h))


def donor(name):
    return Image.open(TEXTURES / (name + ".png")).convert("RGBA")


def remap(target, source, src, dst):
    for origin, (x, y, w, h) in zip(faces(*src), faces(*dst)):
        target.paste(crop(source, origin).resize((w, h), NEAREST), (x, y))


def enderkin():
    thief, saboteur = donor("thief"), donor("saboteur")
    body = Image.new("RGBA", (64, 64))
    eyes = Image.new("RGBA", body.size)
    head = (0, 0, 7, 6, 7)
    remap(body, saboteur, SABOTEUR_HEAD, head)
    remap(eyes, donor("saboteur_eyes"), SABOTEUR_HEAD, head)
    remap(body, thief, THIEF_TORSO, (0, 14, 6, 8, 4))
    for u, length in ((22, 4), (31, 3), (41, 7)):
        remap(body, thief, ARM, (u, 14, 2, length, 2))
    for u, length in ((0, 4), (12, 4), (24, 3)):
        remap(body, thief, LEG, (u, 29, 2, length, 2))
    fin = (0, 40, 3, 1, 5)
    remap(body, saboteur, SABOTEUR_TORSO, fin)
    for u in (30, 43):
        gills = (u, 0, 1, 4, 5)
        remap(body, thief, THIEF_HEAD, gills)
        embellish_gills(body, gills)
    draw = ImageDraw.Draw(body)
    for face in (4, 5):
        x, y, w, h = faces(*fin)[face]
        draw.line((x + 1, y, x + 1, y + h - 1), fill="#302f3e")
        draw.line((x, y + 1, x, y + h - 2), fill="#454652")
        draw.line((x + w - 1, y + 1, x + w - 1, y + h - 2), fill="#3c3c49")
    # Narrow reflected band under the squared Saboteur visor, inside the opaque UV.
    x, y, w, h = faces(*head)[1]
    draw.line((x + 1, y + h - 2, x + w - 2, y + h - 2), fill="#292a36")
    return body, eyes


def embellish_gills(body, uv):
    draw = ImageDraw.Draw(body)
    for face in (0, 2):
        x, y, w, h = faces(*uv)[face]
        for step in range(0, w, 2):
            draw.line((x+step, y, x+step, y+h-2), fill="#444859")
            draw.point((x+step, y+h-1), fill="#2a2934")


def swatches(body):
    return [color for color, _ in Counter(c for c in body.get_flattened_data() if c[-1]).most_common(6)]
