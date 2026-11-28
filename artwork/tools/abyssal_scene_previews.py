"""Render bounded block snapshots with the owned 16-pixel textures and fixed distance fog."""

import argparse
import math
from pathlib import Path
import struct

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
FOG = (20, 51, 62)
WATER = {0, 8, 9}
PARTIAL = {
    3046: ((0, 0, 0, 1, .5, 1),),
    3050: ((0, 0, 0, 1, .1875, 1),),
    3051: ((.125, 0, .125, .875, .75, .875),),
    3042: tuple((x / 16, 0, z / 16, xx / 16, h / 16, zz / 16)
                for x, z, xx, zz, h in ((5, 5, 9, 9, 11), (9, 6, 12, 9, 7), (6, 9, 10, 13, 5))),
}
STAIR = ((.5, .5, 0, 1, 1, 1), (0, .5, 0, .5, 1, 1),
         (0, .5, .5, 1, 1, 1), (0, .5, 0, 1, 1, .5))


def textures():
    atlas = Image.open(ROOT / "source/abyssal/materials.png").convert("RGBA")
    return {
        (column, row): atlas.crop((column * 16, row * 16, column * 16 + 16, row * 16 + 16)).load()
        for row in range(2) for column in range(16)
    }


def tile(block, axis, sign, metadata):
    if 3032 <= block <= 3043:
        if block == 3038 and (axis == 1 or metadata and (axis, sign) != {
            2: (2, -1), 3: (2, 1), 4: (0, -1), 5: (0, 1)
        }.get(metadata)):
            return (8 if axis == 1 else 7), 1
        return {3040: 9, 3041: 10, 3042: 12 if axis == 1 else 11,
                3043: 13}.get(block, block - 3032), 0
    return {
        3044: (4, 0), 3045: (5, 0), 3046: (4, 0), 3047: (1 if axis == 1 else 0, 1),
        3048: (2, 1), 3049: (3, 1), 3050: (4, 1), 3051: (6 if axis == 1 else 5, 1)
    }.get(block, (1 if block in (3, 12, 13) else 0, 0))


def dot(a, b):
    return sum(i * j for i, j in zip(a, b))


def unit(vector):
    length = math.sqrt(dot(vector, vector))
    return tuple(value / length for value in vector)


def pixel(value):
    return max(0, min(15, int(value * 16)))


def cutouts(block, data, above, origin, ray):
    if block == 3041:
        planes = ((.5, .0625, .5, .9375),) if data & 1 else ((.0625, .5, .9375, .5),)
        height = .875
        cell = 10
    elif block == 3039:
        planes = ((.0625, .0625, .9375, .9375), (.9375, .0625, .0625, .9375))
        height = 1
        cell = 7 if above == 3039 else 8
    else:
        planes = ((.125, .125, .875, .875), (.875, .125, .125, .875))
        height, cell = (.5625, 9) if block == 3040 else (.5, 13)
    for x1, z1, x2, z2 in planes:
        dx, dz = x2 - x1, z2 - z1
        perpendicular = ray[0] * dz - ray[2] * dx
        if not perpendicular:
            continue
        distance = ((x1 - origin[0]) * dz - (z1 - origin[2]) * dx) / perpendicular
        x, y, z = (origin[i] + distance * ray[i] for i in range(3))
        across = (x - x1) / dx if dx else (z - z1) / dz
        if 0 <= y <= height and 0 <= across <= 1:
            yield distance, pixel(across), pixel(1 - y / height), cell


def box_hit(bounds, origin, ray):
    enter, leave, normal = -math.inf, math.inf, 0
    for axis, (position, direction) in enumerate(zip(origin, ray)):
        low, high = bounds[axis], bounds[axis + 3]
        if not direction:
            if position < low or position > high:
                return None
            continue
        first, last = (low - position) / direction, (high - position) / direction
        if first > last:
            first, last = last, first
        if first > enter:
            enter, normal = first, axis
        leave = min(leave, last)
        if leave < enter:
            return None
    return enter, normal


def render(blocks, camera, target, path, size=(480, 300)):
    palette = textures()
    forward = unit(tuple(b - a for a, b in zip(camera, target)))
    right = unit((-forward[2], 0, forward[0]))
    up = (right[1] * forward[2] - right[2] * forward[1],
          right[2] * forward[0] - right[0] * forward[2],
          right[0] * forward[1] - right[1] * forward[0])
    image = Image.new("RGB", size, FOG)
    pixels = image.load()
    cell = [math.floor(value) for value in camera]
    width, height = size
    focal = width * 0.89
    for row in range(height):
        elevation = (height / 2 - row - 0.5) / focal
        for col in range(width):
            lateral = (col + 0.5 - width / 2) / focal
            ray = tuple(forward[i] + lateral * right[i] + elevation * up[i] for i in range(3))
            step = [1 if value > 0 else -1 for value in ray]
            delta = [abs(1 / value) if value else math.inf for value in ray]
            side = [(cell[i] + 1 - camera[i] if step[i] > 0 else camera[i] - cell[i]) * delta[i]
                    for i in range(3)]
            x, y, z = cell
            while True:
                axis = min(range(3), key=side.__getitem__)
                distance = side[axis]
                if distance >= 46:
                    break
                side[axis] += delta[axis]
                if axis == 0:
                    x += step[0]
                elif axis == 1:
                    y += step[1]
                else:
                    z += step[2]
                block, metadata, light = blocks.get((x, y, z), (0, 0, 0))
                if block in WATER:
                    continue
                origin = camera[0] - x, camera[1] - y, camera[2] - z
                if block in (3039, 3040, 3041, 3043):
                    above = blocks.get((x, y + 1, z), (0, 0, 0))[0]
                    hits = sorted(cutouts(block, metadata, above, origin, ray))
                    visible = [(d, palette[c, 0][s, t]) for d, s, t, c in hits
                               if d >= distance - 1e-6 and palette[c, 0][s, t][3]]
                    if not visible:
                        continue
                    distance, (r, g, b, alpha) = visible[0]
                    axis = 2
                else:
                    boxes = PARTIAL.get(block)
                    if block in (3044, 3045):
                        boxes = PARTIAL[3046] + (STAIR[metadata & 3],)
                    if boxes:
                        hits = [hit for bounds in boxes if (hit := box_hit(bounds, origin, ray))
                                and hit[0] >= distance - 1e-6]
                        if not hits:
                            continue
                        distance, axis = min(hits)
                    coordinates = [pixel(origin[i] + distance * ray[i]) for i in range(3)]
                    s = coordinates[0 if axis != 0 else 2]
                    t = coordinates[2] if axis == 1 else 15 - coordinates[1]
                    r, g, b, alpha = palette[tile(block, axis, -step[axis], metadata)][s, t]
                if not alpha:
                    continue
                fog = math.exp(-distance * 0.040)
                shade = (0.77, 1.0 if step[1] < 0 else 0.62, 0.91)[axis]
                shade *= min(1.36, 1.01 + 0.065 * light + (0.2 if block == 3038 else 0))
                pixels[col, row] = tuple(int(min(255, value * shade * fog + mist * (1 - fog)))
                                         for value, mist in zip((r, g, b), FOG))
                break
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def snapshot(path):
    """Read ZTO preview volume (bounds, camera pair, then X-fastest packed block/light cells)."""
    data = path.read_bytes()
    x0, y0, z0, nx, ny, nz, *view = struct.unpack_from(">6i6d", data)
    blocks = {}
    for index, (packed,) in enumerate(struct.iter_unpack(">I", data[72:])):
        block = packed & 65535
        if block not in WATER:
            blocks[x0 + index % nx, y0 + index // nx % ny, z0 + index // (nx * ny)] = (
                block, packed >> 16 & 15, packed >> 20 & 15
            )
    render(blocks, view[:3], view[3:], path.with_suffix(".png"))


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("snapshots", nargs="+", type=Path)
    for path in parser.parse_args().snapshots:
        snapshot(path)


if __name__ == "__main__":
    main()
