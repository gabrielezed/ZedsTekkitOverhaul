"""Project the authored parts with the pinned classic mob skins for local review."""

import argparse
from io import BytesIO
from pathlib import Path
import re
from zipfile import ZipFile

from PIL import Image, ImageDraw

from guardian_assets import PREVIEWS, ROOT, SOURCE
from guardian_previews import projection

CLIENT = ROOT / "src/minecraft/zed/tekkitoverhaul/exploration/abyssal/guardian/client"
VIEWS = (
    ("Front", (0, 0, -1), 0), ("Rear", (0, 0, 1), 0),
    ("Side", (-1, 0, 0), 0), ("Three-quarter", (-0.8, -0.4, -1), 0),
    ("Idle swim", (-0.8, -0.3, -1), 1), ("Pursuit", (-0.8, -0.3, -1), 2),
    ("Attack", (-0.8, -0.3, -1), 3), ("Release", (-0.7, -0.1, -1), 4),
)


def native_body(skeletal):
    slim = 1 if skeletal else 2
    return {
        "head": ((0, 0), (0, 0, 0), (-4, -8, -4), (8, 8, 8)),
        "torso": ((16, 16), (0, 0, 0), (-4, 0, -2), (8, 12, 4)),
        "right": ((40, 16), (-5, 2, 0), (-slim, -2, -slim), (2 * slim, 12, 2 * slim)),
        "left": ((40, 16), (5, 2, 0), (-slim, -2, -slim), (2 * slim, 12, 2 * slim)),
        "rightLeg": ((0, 16), (-2, 12, 0), (-slim, 0, -slim), (2 * slim, 12, 2 * slim)),
        "leftLeg": ((0, 16), (2, 12, 0), (-slim, 0, -slim), (2 * slim, 12, 2 * slim)),
    }


def numbers(parameters):
    return [float(value.strip().removesuffix("F")) for value in parameters.split(",")]


def diving_mesh(body):
    source = (CLIENT / "RenderDiver.java").read_text(encoding="utf-8")
    positions = {"goggles": "head", "lensLeft": "head", "lensRight": "head", "mouth": "head",
                 "hose": "head", "tank": "torso", "straps": "torso", "fins": "rightLeg"}
    mesh, parents = {}, {}
    for name, parameters in re.findall(r"^\s*(\w+) = box\(([-0-9F., ]+)\);", source, re.MULTILINE):
        data = numbers(parameters)
        mesh[name] = (tuple(map(int, data[:2])), (0, 0, 0), tuple(data[2:5]), tuple(map(int, data[5:])))
        parents[name] = positions[name]
    for parent, offset, parameters in re.findall(
            r"^\s*(tank|straps)(?:\.setTextureOffset\(([-0-9., ]+)\))?\.addBox\(([-0-9F., ]+)\);",
            source, re.MULTILINE):
        data = numbers(parameters)
        uv = tuple(map(int, numbers(offset))) if offset else mesh[parent][0]
        mesh[parent + "Detail"] = (uv, (0, 0, 0), tuple(data[:3]), tuple(map(int, data[3:])))
        parents[parent + "Detail"] = positions[parent]
    mesh["secondFin"] = mesh["fins"]
    parents["secondFin"] = "leftLeg"
    # Anchor the cosmetic pass at the same pivot chain as the visible native body.
    for parent in set(parents.values()):
        original = body[parent]
        mesh[parent] = (original[0], original[1], original[2], (0, 0, 0))
    return mesh, parents


def caster_mesh():
    source = (CLIENT / "RenderAbyssalEnderkin.java").read_text(encoding="utf-8")
    boxes, parents = {}, {}
    for name, parameters in re.findall(r"^\s*(\w+) = box\(([-0-9F., ]+)\);", source, re.MULTILINE):
        data = numbers(parameters)
        boxes[name] = (tuple(map(int, data[:2])), tuple(data[2:5]), tuple(data[5:8]), tuple(map(int, data[8:])))
    for parent, parameters in re.findall(r"(\w+)\.addChild\(box\(([-0-9F., ]+)\)\);", source):
        data = numbers(parameters)
        name = parent + "Fin"
        boxes[name] = (tuple(map(int, data[:2])), tuple(data[2:5]), tuple(data[5:8]), tuple(map(int, data[8:])))
        parents[name] = parent
    parents.update({child: parent for parent, child in re.findall(r"(\w+)\.addChild\((\w+)\);", source)})
    return boxes, parents


def pose(index, caster, skeletal):
    kick = (0, 0.63, -0.85, 0.46, -0.15)[index]
    right = (-0.3, -0.34, -0.56, -1.57, -1.48)[index] if caster else (-0.55 if skeletal else -1.52)
    if skeletal and index >= 3:
        right = -1.24
    left = -0.35 if caster else -1.05 if skeletal and index >= 3 else -0.25 if skeletal else -1.52
    tilt = -0.09 if index > 0 else 0
    return {"head": (tilt, 0, 0), "torso": (tilt, 0, 0), "right": (right, 0, 0),
            "left": (left, 0, 0.15 if caster else 0), "rightLeg": (0.15 + 0.31 * kick, 0, 0),
            "leftLeg": (0.15 - 0.31 * kick, 0, 0), "gillLeft": (0, -0.19, 0), "gillRight": (0, 0.19, 0)}


def sheet(name, layers, caster=False, skeletal=False):
    image = Image.new("RGB", (880, 440), (18, 31, 41))
    draw = ImageDraw.Draw(image)
    for index, (label, camera, phase) in enumerate(VIEWS):
        texels = []
        for boxes, parents, atlas, cutout in layers:
            texels.extend(projection(boxes, parents, atlas, camera, pose(phase, caster, skeletal), allow_cutout=cutout))
        points = [point for _, polygon, _ in texels for point in polygon]
        cx = (min(x for x, _ in points) + max(x for x, _ in points)) / 2
        cy = (min(y for _, y in points) + max(y for _, y in points)) / 2
        tx, ty = index % 4 * 220 + 110, index // 4 * 220 + 99
        for _, polygon, color in sorted(texels, key=lambda cell: cell[0]):
            draw.polygon([(round(tx + (x - cx) * 5), round(ty + (y - cy) * 5)) for x, y in polygon], fill=color)
        draw.text((tx, ty + 88), label, anchor="mt", fill=(175, 209, 213))
    image.save(PREVIEWS / (name + "-model-contact.png"))


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("pack", type=Path, help="local effective Minecraft 1.2.5 client archive")
    args = parser.parse_args()
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    gear = Image.open(SOURCE / "diver_equipment.png").convert("RGBA")
    with ZipFile(args.pack) as archive:
        for name in ("zombie", "skeleton"):
            native = Image.open(BytesIO(archive.read("mob/" + name + ".png"))).convert("RGBA")
            body = native_body(name == "skeleton")
            mesh, attachments = diving_mesh(body)
            sheet("diver-" + name, ((body, {}, native, True), (mesh, attachments, gear, False)), skeletal=name == "skeleton")
    body, bones = caster_mesh()
    atlas = Image.open(SOURCE / "enderkin.png").convert("RGBA")
    sheet("abyssal-enderkin", ((body, bones, atlas, False),), caster=True)
