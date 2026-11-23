"""Build the local machine art from the original Tekkit Classic sprites."""
import argparse
from dataclasses import dataclass
from hashlib import sha256
from pathlib import Path

from PIL import Image

from alpha1_previews import BC, IC2, PARTS, SOURCE, cube, image, sheet
from textures import ART, CACHE, read_json, save_image, sprite, verified, write_json

MANIFEST = ART / "manifests/machine-derivations.json"
ASSETS = ART / "manifests/assets.json"
ATLAS = "distillation/distillation_blocks.png"
REFINERY = BC.parents[2] / "factory/gui/refinery.png"
STEEL = "#3d4b50"
AIR = "#7494a2"
PALE = "#c4dce0"


@dataclass(frozen=True)
class Donor:
    atlas: Path
    cell: tuple

    def pixels(self):
        return sprite(self.atlas, *self.cell, 16)

    def record(self):
        cache, atlas = self.atlas.relative_to(CACHE).as_posix().split("/", 1)
        return {
            "reference": cache,
            "atlas": atlas,
            "cell": list(self.cell),
            "rgbaSha256": sha256(self.pixels().tobytes()).hexdigest(),
        }


@dataclass
class Face:
    base: Donor
    pixels: Image.Image
    operations: str
    overlays: tuple

    @classmethod
    def copy(cls, base, operations, *overlays):
        return cls(base, base.pixels().copy(), operations, overlays)

    def record(self):
        original = self.base.pixels()
        changed = sum(before != after for before, after in
                      zip(original.get_flattened_data(), self.pixels.get_flattened_data()))
        bounds = difference_mask(original, self.pixels).getbbox()
        return {
            "base": self.base.record(),
            "overlays": [donor.record() for donor in self.overlays],
            "operations": self.operations,
            "retainedBasePixels": 256 - changed,
            "editedBounds": list(bounds) if bounds else None,
            "outputRgbaSha256": sha256(self.pixels.tobytes()).hexdigest(),
        }


def ic2(col, row):
    return Donor(IC2, (col, row))


def bc(col, row):
    return Donor(BC, (col, row))


def mark(target, x, y, rows, palette):
    """Place a small hand-edited mark. Dots leave the donor pixel visible."""
    for dy, row in enumerate(rows):
        for dx, key in enumerate(row):
            if key != ".":
                color = palette[key].lstrip("#")
                target.putpixel((x + dx, y + dy), (*bytes.fromhex(color), 255))


def tint(target, box, select, transform):
    for y in range(box[1], box[3]):
        for x in range(box[0], box[2]):
            red, green, blue, alpha = target.getpixel((x, y))
            if alpha and select(red, green, blue):
                target.putpixel((x, y), (*transform(red, green, blue), alpha))


def difference_mask(before, after):
    mask = Image.new("L", (16, 16))
    mask.putdata([255 if old != new else 0 for old, new in
                  zip(before.get_flattened_data(), after.get_flattened_data())])
    return mask


def comparison_faces(faces, include_cubes=True):
    entries = []
    for label, face in faces:
        source = face.base.pixels()
        mask = difference_mask(source, face.pixels)
        diff = Image.composite(Image.new("RGBA", (16, 16), "#e48851"), source, mask)
        entries.extend(((label + " original", source), (label + " final", face.pixels),
                        (label + " edits", diff)))
    if include_cubes:
        front, side, top = [face for _, face in faces[:3]]
        entries.extend((("Original donor cube", cube(*(face.base.pixels() for face in (front, side, top)))),
                        ("Derived cube", cube(front.pixels, side.pixels, top.pixels))))
    return entries


def compressor():
    front = Face.copy(ic2(5, 3), "Keep the compressor bezel and shaded shell. Replace the inner drive with three air louvers and add a six-pixel-wide pressure dial above it.")
    mark(front.pixels, 4, 8, ("..SSSS..", ".LLLLLL.", ".SSSSSS.", ".LLLLLL.", ".SSSSSS.", "..LLLL.."),
         {"S": "#22282c", "L": "#536e7b"})
    mark(front.pixels, 5, 1, (".SSSS.", "SPhhDS", "SPDhDS", ".SSSS."),
         {"S": STEEL, "P": PALE, "h": "#a7b8b9", "D": "#536b76"})
    side = Face.copy(ic2(5, 4), "Keep the original shell. Add a three-pixel service witness mark near the lower right.")
    mark(side.pixels, 11, 11, ("SPD",), {"S": STEEL, "P": AIR, "D": "#576e79"})
    top = Face.copy(ic2(5, 1), "Keep the original socket and cap. Color just the outer socket rim blue-grey.")
    tint(top.pixels, (4, 4, 12, 12), lambda r, g, b: (r, g, b) == (86, 93, 94),
         lambda r, g, b: (74, 100, 112))
    return [("Front", front), ("Side", side), ("Top", top)]


def compressor_previews(faces):
    entries = comparison_faces(faces)
    sheet("compressor-original-vs-zto.png", entries, 8, 3)
    sheet("compressor-native.png", entries, 1, 3)


def casing(col):
    return [Face.copy(ic2(col, row), "Reuse the original IC2 casing face as-is.") for row in (3, 4, 1, 0, 2)]


def boiler():
    faces = casing(3)
    front = faces[0]
    front.operations = "Keep the Macerator hatch and frame. Add a copper latch and a dark petroleum sight slot inside the hatch."
    mark(front.pixels, 5, 8, ("......", "PPP.LH", "SSS.LD", "....DD"),
         {"P": "#66513e", "S": "#413830", "L": "#a77b50", "H": "#c3a37a", "D": "#755237"})
    return faces


def heater():
    faces = casing(2)
    front = faces[0]
    front.operations = "Keep the Electric Furnace intake and lower vent. Add a short copper heating strip inside the intake."
    mark(front.pixels, 5, 4, ("B.B.B.", "RTRTRB"), {"B": "#704236", "R": "#9b542f", "T": "#c17d46"})
    return faces


def port(base, face=None):
    pump = bc(3, 6)
    operation = "Copy the BuildCraft Pump boss onto the centerline. Omit the flat field and lower pipe, and desaturate the olive ring to aged brass."
    if face is None:
        face = Face.copy(base, operation, pump)
    else:
        face.operations += " " + operation
        face.overlays += (pump,)
    fitting = pump.pixels().crop((3, 3, 13, 12))
    fitting.putdata([
        (0, 0, 0, 0) if (r, g, b) == (204, 204, 204)
        else (r - 12, g - 27, (r + b) // 2, a) if r > b + 35
        else (r, g, b, a)
        for r, g, b, a in fitting.get_flattened_data()
    ])
    face.pixels.alpha_composite(fitting, (3, 3))
    return face


def column_front(upper, sampling_tap=True):
    tank = bc(int(upper), 6)
    iron = bc(3, 7)
    face = Face.copy(ic2(5, 4), "Darken the central IC2 steel into a vessel recess. Overlay the BuildCraft Tank frame in dark steel and splice in Iron Waterproof Pipe ribs as collars.", tank, iron)
    tint(face.pixels, (2, 0, 14, 16), lambda r, g, b: True,
         lambda r, g, b: (r // 2, g // 2 + 3, b // 2 + 5))
    window = tank.pixels()
    tint(window, (0, 0, 16, 16), lambda r, g, b: r == 127 and g == 0,
         lambda r, g, b: (72, 83, 87))
    tint(window, (0, 0, 16, 16), lambda r, g, b: r == g == b == 255,
         lambda r, g, b: (155, 172, 176))
    face.pixels.alpha_composite(window)
    ribs = iron.pixels().crop((0, 4, 16, 6))
    tint(ribs, (0, 0, 16, 2), lambda r, g, b: True,
         lambda r, g, b: (119, 132, 137) if r > 200 else (74, 85, 90))
    face.pixels.alpha_composite(ribs, (0, 0))
    face.pixels.alpha_composite(ribs, (0, 14))
    if upper and sampling_tap:
        face.operations += " Add a small upper sampling tap."
        mark(face.pixels, 6, 6, (".HH.", "HBBD", ".DD.", ".D.."),
             {"H": "#b39a73", "B": "#91764e", "D": "#545050"})
    return face


def column():
    faces = casing(5)
    faces[0] = column_front(False)
    faces[1] = port(faces[1].base, column_front(True, sampling_tap=False))
    faces[4] = column_front(True, sampling_tap=False)
    return faces + [column_front(True)]


def condenser():
    faces = casing(8)
    faces[1] = port(ic2(8, 4))
    radiator = Donor(REFINERY, (2, 1))
    front = Face.copy(ic2(8, 3), "Keep the two IC2 Pump chambers. Soften their blue fill and add a cyan BuildCraft Refinery magnet strip above the chambers.", radiator)
    tint(front.pixels, (2, 8, 14, 12), lambda r, g, b: b > r + 35,
         lambda r, g, b: ((r + g) // 2, g, (g + b) // 2))
    front.pixels.alpha_composite(radiator.pixels().crop((5, 5, 11, 8)), (5, 1))
    faces[0] = front
    return faces


def receiver():
    faces = casing(6)
    tank = bc(0, 6)
    front = Face.copy(ic2(6, 2), "Recess the Canner shell and overlay the original Tank glass highlights and graduated frame. Tint the frame to copper and add a low amber fill behind the glass.", tank)
    tint(front.pixels, (2, 0, 14, 16), lambda r, g, b: True,
         lambda r, g, b: (r // 3, g // 3 + 3, b // 3 + 5))
    tint(front.pixels, (3, 9, 13, 15), lambda r, g, b: True,
         lambda r, g, b: (r + 43, g + 21, b - 17))
    window = tank.pixels()
    tint(window, (0, 0, 16, 16), lambda r, g, b: r == 127 and g == 0,
         lambda r, g, b: (125, 95, 66))
    front.pixels.alpha_composite(window)
    faces[0] = front
    faces[1] = port(ic2(6, 4))
    return faces


def thermometer():
    faces = casing(6)
    socket = ic2(5, 1)
    front = Face.copy(ic2(6, 2), "Keep the Canner shell. Transfer the Compressor cap socket as the instrument bezel and inset an ivory dial with a red needle.", socket)
    bezel = socket.pixels().crop((4, 4, 12, 12))
    bezel.putdata([pixel if pixel[0] <= 86 else (0, 0, 0, 0)
                   for pixel in bezel.get_flattened_data()])
    front.pixels.alpha_composite(bezel, (4, 4))
    mark(front.pixels, 6, 6, ("IIII", "IIRA", "IRAA", "AAAA"),
         {"I": "#dfddc2", "R": "#ad4540", "A": "#b9b9a5"})
    faces[0] = front
    return faces


def outputs(compressor_faces, parts):
    result = {}
    for (_, face), name in zip(compressor_faces, ("grille", "side", "cap")):
        result[f"underwater/compressor_{name}.png"] = {None: face}
    result[ATLAS] = {(x, y): face for x, (_, faces) in enumerate(parts) for y, face in enumerate(faces)}
    return result


def assembled_image(parts):
    faces = dict(parts)
    layout = (
        ("Column", 5, "Condenser"),
        ("Column", 0, "Receiver"),
        ("Boiler", 0, "Thermometer"),
        ("Heater", 0, None),
    )
    elevation = Image.new("RGBA", (32, 64))
    for y, (left, row, right) in enumerate(layout):
        elevation.paste(faces[left][row].pixels, (0, 16 * y))
        if right:
            elevation.paste(faces[right][0].pixels, (16, 16 * y))
    return elevation


def reference_previews():
    groups = (("Machine", 0), ("Electric Furnace", 2), ("Macerator", 3), ("Extractor", 4),
              ("Compressor", 5), ("Canner", 6), ("IC2 Pump", 8))
    entries = [(f"{name} {label} {col},{row}", ic2(col, row).pixels())
               for name, col in groups
               for label, row in (("front", 3), ("side", 4), ("top", 1), ("rear", 2), ("base", 0))]
    fittings = (("Tank side", 0, 6), ("Tank extension", 1, 6), ("Tank cap", 2, 6),
                ("Pump side", 3, 6), ("Pump base", 4, 6), ("Pump top", 5, 6),
                ("Iron liquid pipe", 3, 7), ("Iron plain pipe", 3, 1),
                ("Gold liquid pipe", 4, 7), ("Stone liquid pipe", 2, 7))
    entries.extend((f"BC {label} {x},{y}", bc(x, y).pixels()) for label, x, y in fittings)
    entries.extend((f"Refinery UV {x},{y}", Donor(REFINERY, (x, y)).pixels())
                   for x, y in ((0, 1), (1, 1), (2, 1)))
    sheet("industrial-source-sprites.png", entries, 8, 5)
    sheet("refinery-source-uv.png", [("BuildCraft Refinery model UV", image(REFINERY))], 8, 1)


def distillation_previews(parts, compressor_faces):
    entries = []
    machines = []
    for name, faces in parts:
        labels = ("Front", "Side", "Top", "Bottom", "Rear", "Upper front")
        comparisons = comparison_faces([(f"{name} {label}", face) for label, face in zip(labels, faces)])
        while len(comparisons) % 3:
            comparisons.append(("", Image.new("RGBA", (16, 16))))
        entries.extend(comparisons)
        machines.append((name, cube(*(face.pixels for face in faces[:3]))))
    reference_cubes = [
        ("IC2 Compressor", cube(*(face.base.pixels() for _, face in compressor_faces))),
        ("ZTO Air Compressor", cube(*(face.pixels for _, face in compressor_faces))),
        ("IC2 Electric Furnace", cube(*(ic2(2, row).pixels() for row in (3, 4, 1)))),
        ("IC2 Macerator", cube(*(ic2(3, row).pixels() for row in (3, 4, 1)))),
        ("BuildCraft Pump", cube(*(bc(col, 6).pixels() for col in (3, 3, 5)))),
        ("BuildCraft Tank", cube(*(bc(col, 6).pixels() for col in (0, 0, 2)))),
    ]
    sheet("distillation-original-vs-zto.png", entries, 8, 6)
    sheet("distillation-native.png", entries, 1, 6)
    sheet("distillation-complete-apparatus.png", [("Front elevation", assembled_image(parts))], 8, 1)
    sheet("industrial-machines-comparison.png", reference_cubes + machines, 8, 6)


def references():
    result = {}
    for key, mod, version, paths in (
        ("ic2", "IndustrialCraft 2", "1.97", (IC2,)),
        ("buildcraft-core", "BuildCraft", "2.2.14", (BC, REFINERY)),
    ):
        index = read_json(CACHE / key / "index.json")
        atlases = {}
        for path in paths:
            relative = path.relative_to(CACHE / key).as_posix()
            checksum = index["resources"][relative]
            verified(path.read_bytes(), checksum, sha256, relative)
            atlases[relative] = checksum
        result[key] = {"mod": mod, "version": version, "archive": index["archive"], "atlases": atlases}
    return result


def build_assets(files, assets):
    atlas_cells = {tuple(cell) for cell in assets[f"source/{ATLAS}"]["opaqueCells"]}
    if atlas_cells != set(files[ATLAS]):
        raise ValueError("Distillation cells differ from the runtime asset manifest")

    manifest = {"references": references(), "outputs": {}}
    images = {}
    for path, faces in files.items():
        image_size = tuple(assets[f"source/{path}"]["size"])
        result = Image.new("RGBA", image_size)
        records = {}
        for cell, face in faces.items():
            label = "image" if cell is None else f"{cell[0]},{cell[1]}"
            record = face.record()
            if face.pixels.size != (16, 16) or face.pixels.getchannel("A").getextrema() != (255, 255):
                raise ValueError(f"Non-opaque or invalid face: {path} {label}")
            retained = record["retainedBasePixels"]
            if not retained or (retained == 256 and not face.operations.startswith("Reuse")):
                raise ValueError(f"Unexpected source pixel retention: {path} {label}")
            records[label] = record
            result.paste(face.pixels, tuple(axis * 16 for axis in (cell or (0, 0))))
        images[path] = result
        manifest["outputs"][path] = records
    return manifest, images


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("build", "check", "record", "references"))
    args = parser.parse_args()
    if args.command == "references":
        references()
        reference_previews()
        return

    compressor_faces = compressor()
    parts = list(zip(PARTS, (boiler(), heater(), column(), condenser(), receiver(), thermometer())))
    asset_list = read_json(ASSETS)
    assets = {entry["source"]: entry for entry in asset_list}
    manifest, images = build_assets(outputs(compressor_faces, parts), assets)
    if args.command == "record":
        write_json(MANIFEST, manifest)
    elif read_json(MANIFEST) != manifest:
        raise ValueError("Machine derivations differ from the recorded donor hashes and pixel edits")

    for path, expected in images.items():
        asset = assets[f"source/{path}"]
        if args.command == "check":
            current = image(SOURCE / path)
            if current.size != expected.size or current.tobytes() != expected.tobytes():
                raise ValueError(f"Machine artwork differs from the recorded derivation: {path}")
            if asset.get("derivation") != MANIFEST.relative_to(ART).as_posix() or asset["origin"] != "local-derivative":
                raise ValueError(f"Missing local derivation status: {path}")
        else:
            save_image(expected, SOURCE / path)
            asset["origin"] = "local-derivative"
            asset["derivation"] = MANIFEST.relative_to(ART).as_posix()
    if args.command == "check":
        print(f"Verified {sum(map(len, manifest['outputs'].values()))} derived machine faces")
    else:
        write_json(ASSETS, asset_list)
        compressor_previews(compressor_faces)
        reference_previews()
        distillation_previews(parts, compressor_faces)


if __name__ == "__main__":
    main()
