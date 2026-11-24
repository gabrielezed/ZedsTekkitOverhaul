"""Create orthographic layout and panel mockups for the alpha.1 refinery review."""
import argparse
import re
from io import BytesIO
from pathlib import Path
from zipfile import ZipFile

from PIL import Image, ImageDraw

from textures import ART, RUNTIME, artwork_sheet, save_image

SOURCE = ART / "source"
OUT = ART / "previews"
ATLAS = SOURCE / "distillation/distillation_blocks.png"
INDICATORS = SOURCE / "distillation/distillation_indicators.png"
GEOMETRY = RUNTIME / "zed/tekkitoverhaul/distillation/DistillationGeometry.java"
VANILLA = ART / "references/downloads/minecraft-1.2.5.jar"
B, H, C, K, R, T = range(6)
PARTS = (B, H, C, C, K, R, T)
CURRENT = ((0, 0, 0), (0, -1, 0), (0, 1, 0), (0, 2, 0), (1, 2, 0), (1, 1, 0), (1, 0, 0))
LAYOUTS = (
    ("current", "Current positions and cube profiles", CURRENT, False),
    ("compact", "A. Compact tower", CURRENT, True),
    ("lshape", "B. L-shaped refinery",
     CURRENT[:4] + ((0, 2, 1), (0, 1, 1), (1, 0, 1)), True),
    ("horizontal", "C. Factory-floor line",
     CURRENT[:2] + ((1, 0, 0), (2, 0, 0), (3, 0, 0), (4, 0, 0), (5, 0, 0)), True),
)


def cell(sheet, x, y):
    return sheet.crop((x * 16, y * 16, (x + 1) * 16, (y + 1) * 16))


def model_boxes():
    source = GEOMETRY.read_text()

    def read(lines):
        return [tuple(map(int, match.split(", "))) for match in
                re.findall(r"new Box\(([\d, ]+), [^,]+, (?:true|false)\)", lines)]

    boxes = {}
    for label, part in (("COLUMN", C), ("CONDENSER", K), ("RECEIVER", R)):
        body, = read(re.search(rf"{label}_BODY = .+;", source).group())
        shell = read("\n".join(re.findall(rf"{label}_(?:BASE|CAP) = .+;", source)))
        if part == C:
            shell.extend(read(source.split("COLUMN_RAILS = {")[1].split("};")[0]))
        boxes[part] = [(body, False)] + [(box, True) for box in shell]
    return boxes


def surface(sheet, part, index, view, model, adjacent):
    shade = Image.new("RGBA", (16, 16))
    if part not in model:
        row = {"front": 5 if index == 3 else 0, "side": 1, "top": 2}[view]
        return cell(sheet, part, row)
    boxes = list(model[part])
    # For assembled elevations, show the center coupling and receiver neck.
    if adjacent and part in (C, K) and index in (3, 4):
        outward = (13, 7, 6, 16, 11, 10) if part == C else (0, 7, 6, 2, 11, 10)
        boxes.append((outward, True))
    if adjacent and part == R:
        boxes.append(((6, 14, 6, 10, 16, 10), True))
    depth = {"front": lambda box: box[5], "side": lambda box: box[3], "top": lambda box: box[4]}[view]
    for box, trim in sorted(boxes, key=lambda entry: depth(entry[0])):
        xmin, ymin, zmin, xmax, ymax, zmax = box
        if view == "top":
            area, row = (xmin, zmin, xmax, zmax), 2
        else:
            inner = (xmin, xmax) if view == "front" else (zmin, zmax)
            area = (inner[0], 16 - ymax, inner[1], 16 - ymin)
            row = 2 if trim else 1 if view == "side" else 5 if index == 3 else 0
        crop = cell(sheet, part, row).crop(area)
        shade.paste(crop, area[:2])
    return shade


def projection(sheet, placements, view, model, assembled=False, state=None):
    xs, ys, zs = zip(*placements)
    width, height = {
        "front": (max(xs) + 1, max(ys) - min(ys) + 1),
        "side": (max(zs) + 1, max(ys) - min(ys) + 1),
        "top": (max(xs) + 1, max(zs) + 1),
    }[view]
    picture = Image.new("RGBA", (16 * width, 16 * height))
    depth = {"front": 2, "side": 0, "top": 1}[view]
    for index, (x, y, z) in sorted(enumerate(placements), key=lambda item: item[1][depth],
                                  reverse=view == "front"):
        part = PARTS[index]
        position = {"front": (x, max(ys) - y), "side": (z, max(ys) - y), "top": (x, z)}[view]
        pixel = (16 * position[0], 16 * position[1])
        sprite = surface(sheet, part, index, view, model, assembled)
        if state and view == "front":
            overlays = {
                H: (2, 0, (5, 8, 11, 11)), C: (2 if index == 3 else 0, 1, (6, 3, 10, 13)),
                K: (1, 2, (5, 9, 11, 12)), R: (4 if state == "running" else 9, 3, (6, 5, 10, 12)),
                T: (2 if state == "running" else 5, 4, (5, 12, 11, 14)),
            }
            if part in overlays and (state == "running" or part in (R, T)):
                frame, row, area = overlays[part]
                overlay = cell(Image.open(INDICATORS), frame, row)
                sprite.paste(overlay.resize((area[2] - area[0], area[3] - area[1]), Image.Resampling.NEAREST),
                             area[:2])
        picture.alpha_composite(sprite, pixel)
    return picture


class LegacyFont:
    def __init__(self, asset, chars):
        self.asset = asset
        self.chars = chars

    def show(self, image, position, text, color):
        x, y = position
        for char in text:
            if char == " ":
                x += 4
                continue
            index = self.chars.index(char) + 32
            left, top = index % 16 * 8, index // 16 * 8
            mask = self.asset.crop((left, top, left + 8, top + 8)).getchannel("A")
            bounds = mask.getbbox()
            image.paste(color, (x, y, x + 8, y + 8), mask)
            x += bounds[2] + 1
        return x - position[0]

    def width(self, text):
        return self.show(Image.new("RGBA", (1, 1)), (0, 0), text, "#000000")


def panel_mockups():
    if not VANILLA.is_file():
        print("GUI mockups require the locally imported vanilla archive.")
        return
    with ZipFile(VANILLA) as jar:
        chars = "".join(line for line in jar.read("font.txt").decode("utf-8").splitlines()
                        if not line.startswith("#"))
        font = LegacyFont(Image.open(BytesIO(jar.read("font/default.png"))).convert("RGBA"), chars)
        controls = Image.open(BytesIO(jar.read("gui/gui.png"))).convert("RGBA")

    def label(img, y, value, color="#30302e"):
        font.show(img, (14, y), value, color)

    def gauge(img, y, name, value, fill, color):
        label(img, y, name)
        font.show(img, (232 - font.width(value), y), value, "#30302e")
        draw = ImageDraw.Draw(img)
        draw.rectangle((14, y + 10, 231, y + 16), fill="#53524d")
        draw.rectangle((15, y + 11, 230, y + 15), fill="#74716a")
        if fill:
            draw.rectangle((15, y + 11, 14 + int(fill * 216), y + 15), fill=color)

    def button(img, x, y, width, value, disabled=False):
        row, mid = (46 if disabled else 66), width // 2
        img.paste(controls.crop((0, row, mid, row + 20)), (x, y))
        img.paste(controls.crop((200 - mid, row, 200, row + 20)), (x + mid, y))
        font.show(img, (x + (width - font.width(value)) // 2, y + 6), value,
                  "#a0a0a0" if disabled else "#e0e0e0")

    boiler = Image.open(SOURCE / "interfaces/distillation.png").crop((0, 0, 246, 206))
    thermo = boiler.copy()
    for image, title in ((boiler, "Distillation Boiler"), (thermo, "Distillation Thermometer")):
        label(image, 11, title)
        label(image, 32, "Formed  /  Processing", "#284c38")
        button(image, 165, 177, 67, "Close")
    label(boiler, 48, "Kerosene  /  Target 205 C")
    gauge(boiler, 62, "Crude oil", "4250 / 8000 mB", 4250 / 8000, "#926844")
    gauge(boiler, 82, "Cooling water", "5000 / 8000 mB", 5000 / 8000, "#5283a3")
    gauge(boiler, 102, "Kerosene", "3000 / 8000 mB", 3000 / 8000, "#64834f")
    gauge(boiler, 122, "Batch progress", "92 / 200 ticks", 92 / 200, "#577d69")
    label(thermo, 55, "Kerosene  /  Target 205 C")
    for index, option in enumerate(("Gasoline 125 C", "Kerosene 205 C", "Diesel 300 C", "Heavy Gas Oil 380 C")):
        button(thermo, 14 + index % 2 * 111, 78 + index // 2 * 23, 107, option, index == 1)
    gauge(thermo, 130, "Batch progress", "92 / 200 ticks", 92 / 200, "#577d69")
    save_image(artwork_sheet([("Boiler", boiler), ("Thermometer", thermo)], 2, 2), OUT / "distillation-panels.png")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--atlas", type=Path, default=ATLAS)
    parser.add_argument("--comparison", type=Path, help="Write only a comparison using this invocation's atlas.")
    args = parser.parse_args()
    sheet = Image.open(args.atlas).convert("RGBA")
    model = model_boxes()
    fronts = []
    for key, title, positions, shaped in LAYOUTS:
        volumes = model if shaped else {}
        views = [(view.capitalize(), projection(sheet, positions, view, volumes, key == "compact"))
                 for view in ("front", "side", "top")]
        fronts.append((title + " (side)", views[1][1]) if key == "lshape" else (title, views[0][1]))
        if not args.comparison:
            save_image(artwork_sheet(views, 5, 3), OUT / f"distillation-layout-{key}.png")
    if args.comparison:
        save_image(artwork_sheet(fronts, 4, 2), args.comparison)
        return

    states = [("Idle", None), ("Running", "running"), ("Output blocked", "blocked")]
    activity = [(label, projection(sheet, CURRENT, "front", model, True, state)) for label, state in states]
    save_image(artwork_sheet([fronts[0]] + activity, 6, 4), OUT / "distillation-state-preview.png")
    panel_mockups()


if __name__ == "__main__":
    main()
