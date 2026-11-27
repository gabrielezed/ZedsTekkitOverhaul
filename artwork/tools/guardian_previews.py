"""Project the runtime Guardian cuboids and authored texels in seven reference poses."""

from math import cos, sin, sqrt
import re

from PIL import Image, ImageDraw

from guardian_assets import PREVIEWS, ROOT, SOURCE


MODEL = ROOT / ("src/minecraft/zed/tekkitoverhaul/exploration/abyssal/"
                "guardian/client/RenderAbyssalGuardian.java")
VIEWS = (
    ("Front", (0, 0, -1), {}),
    ("Side", (-1, 0, 0), {}),
    ("Rear", (0, 0, 1), {}),
    ("Three-quarter", (-0.8, -0.6, -1), {}),
    ("Dorsal", (0, -1, -0.01), {}),
    ("Cruising", (-0.8, -0.3, -1), {"body": (-0.22, 0, 0), "peduncle": (0, 0.32, 0),
        "tail": (0, 0.31, 0), "port": (0, -0.24, -0.4), "starboard": (0, 0.24, 0.4)}),
    ("Lunge", (-0.8, -0.3, -1), {"jaw": (0.58, 0, 0), "port": (0, -0.7, -0.25),
        "starboard": (0, 0.7, 0.25), "peduncle": (0, 0.3, 0), "tail": (0, 0.31, 0)}),
)


def add(a, b):
    return tuple(left + right for left, right in zip(a, b))


def dot(a, b):
    return sum(left * right for left, right in zip(a, b))


def cross(a, b):
    x, y, z = a
    u, v, w = b
    return (y * w - z * v, z * u - x * w, x * v - y * u)


def unit(a):
    length = sqrt(dot(a, a))
    return tuple(value / length for value in a)


def rotate(point, angles):
    x, y, z = point
    rx, ry, rz = angles
    y, z = y * cos(rx) - z * sin(rx), y * sin(rx) + z * cos(rx)
    x, z = x * cos(ry) + z * sin(ry), -x * sin(ry) + z * cos(ry)
    return (x * cos(rz) - y * sin(rz), x * sin(rz) + y * cos(rz), z)


def model():
    source = MODEL.read_text(encoding="utf-8")
    boxes = {}
    for name, parameters in re.findall(r"^\s*(\w+) = box\(([-0-9F., ]+)\);", source, re.MULTILINE):
        values = [float(value.strip().removesuffix("F")) for value in parameters.split(",")]
        boxes[name] = (tuple(map(int, values[:2])), tuple(values[2:5]), tuple(values[5:8]), tuple(map(int, values[8:])))
    parents = {child: parent for parent, child in re.findall(r"(\w+)\.addChild\((\w+)\);", source)}
    return boxes, parents


def faces(size, uv):
    w, h, d = size
    u, v = uv
    # The serpentine UV strip follows the six native ModelBox quads.
    return (
        ((w, 0, d), (0, 0, -1), (0, 1, 0), (1, 0, 0), d, h, u + d + w, v + d),
        ((0, 0, 0), (0, 0, 1), (0, 1, 0), (-1, 0, 0), d, h, u, v + d),
        ((0, 0, 0), (1, 0, 0), (0, 0, 1), (0, -1, 0), w, d, u + d, v),
        ((0, h, d), (1, 0, 0), (0, 0, -1), (0, 1, 0), w, d, u + d + w, v),
        ((w, 0, 0), (-1, 0, 0), (0, 1, 0), (0, 0, -1), w, h, u + d, v + d),
        ((0, 0, d), (1, 0, 0), (0, 1, 0), (0, 0, 1), w, h, u + 2 * d + w, v + d),
    )


def projection(boxes, parents, atlas, camera, pose, allow_cutout=False):
    forward = unit(camera)
    right = unit(cross((0, -1, 0), forward))
    down = cross(right, forward)
    light = unit((-0.5, -1, -0.6))
    texels = []

    def placed(name, point, vector=False):
        while name is not None:
            point = rotate(point, pose.get(name, (0, 0, 0)))
            if not vector:
                offset = boxes[name][1] if name != "body" else (0, 0, 0)
                point = add(point, offset)
            name = parents.get(name)
        return point

    for name, (uv, _, origin, size) in boxes.items():
        for corner, horizontal, vertical, normal, width, height, u, v in faces(size, uv):
            normal = placed(name, normal, vector=True)
            if dot(normal, forward) <= 0:
                continue
            shade = 0.6 + max(0, dot(light, normal)) * 0.4
            origin_face = add(origin, corner)
            for row in range(height):
                for column in range(width):
                    rgb = atlas.getpixel((u + column, v + row))
                    if rgb[3] != 255:
                        if allow_cutout and rgb[3] == 0:
                            continue
                        raise ValueError(f"Transparent texel in {name} face at {u + column},{v + row}")
                    points = []
                    depth = 0
                    for dx, dy in ((0, 0), (1, 0), (1, 1), (0, 1)):
                        point = placed(name, tuple(origin_face[i] + horizontal[i] * (column + dx)
                                                   + vertical[i] * (row + dy) for i in range(3)))
                        points.append((dot(point, right), dot(point, down)))
                        depth += dot(point, forward)
                    texels.append((depth, points, tuple(round(shade * component) for component in rgb[:3])))
    return texels


def main():
    boxes, parents = model()
    image = Image.new("RGB", (800, 420), (19, 31, 38))
    draw = ImageDraw.Draw(image)
    atlas = Image.open(SOURCE / "guardian.png").convert("RGBA")
    for i, (label, camera, pose) in enumerate(VIEWS):
        texels = projection(boxes, parents, atlas, camera, pose)
        points = [point for _, polygon, _ in texels for point in polygon]
        center_x = (min(x for x, _ in points) + max(x for x, _ in points)) / 2
        center_y = (min(y for _, y in points) + max(y for _, y in points)) / 2
        tile_x, tile_y = i % 4 * 200, i // 4 * 210
        for _, polygon, rgb in sorted(texels, key=lambda item: item[0]):
            draw.polygon([(round(tile_x + 100 + (x - center_x) * 4.5),
                           round(tile_y + 89 + (y - center_y) * 4.5)) for x, y in polygon], fill=rgb)
        draw.text((tile_x + 100, tile_y + 177), label, anchor="mt", fill=(182, 213, 211))
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    image.save(PREVIEWS / "guardian-model-contact.png")


if __name__ == "__main__":
    main()
