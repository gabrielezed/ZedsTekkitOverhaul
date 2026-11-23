"""Generate local alpha.1 comparisons. The cube projection is a static artwork preview."""
from PIL import Image

from textures import ART, artwork_sheet, save_image, sprite

OUT = ART / "previews"
SOURCE = ART / "source"
BEFORE = OUT / "before"
VANILLA = ART / "references/vanilla/terrain.png"
IC2 = ART / "references/ic2/ic2/sprites/block_machine.png"
BC = ART / "references/buildcraft-core/net/minecraft/src/buildcraft/core/gui/block_textures.png"
SHEET = SOURCE / "distillation/distillation_blocks.png"
PARTS = ("Boiler", "Heater", "Column", "Condenser", "Receiver", "Thermometer")


def image(path):
    with Image.open(path) as opened:
        return opened.convert("RGBA")


def cube(front, side, top):
    """Project three faces into a 16px swatch. Keep face source PNGs available for exact colors."""
    result = Image.new("RGBA", (16, 16))
    for y in range(16):
        for x in range(16):
            px, py = x + 0.5, y + 0.5
            left = px < 8
            top_u, top_v = py / 8 - (px - 8) / 16, py / 8 + (px - 8) / 16
            if 0 <= top_u < 1 and 0 <= top_v < 1:
                u, v = top_u, top_v
                texture, light = top, 1.0
            else:
                u = px / 8 if left else (px - 8) / 8
                seam = 4 + 4 * u if left else 8 - 4 * u
                v = (py - seam) / 8
                if not 0 <= v < 1:
                    continue
                texture, light = (front, 0.91) if left else (side, 0.72)
            pixel = texture.getpixel((int(u * 16), int(v * 16)))
            result.putpixel((x, y), (*(int(channel * light) for channel in pixel[:3]), pixel[3]))
    return result


def machine(path, front, side, cap):
    return cube(*(sprite(path, x, y, 16) for x, y in (front, side, cap)))


def sheet(filename, entries, scale, columns):
    save_image(artwork_sheet(entries, scale, columns), OUT / filename)


def before_after(filename, pairs, scale, columns):
    entries = []
    for old_path, new_path in pairs:
        old = BEFORE / old_path
        new = SOURCE / new_path
        if not old.is_file():
            continue
        old_image, new_image = image(old), image(new)
        if new.name == "air_compressor.png":
            old_image, new_image = (im.crop((0, 0, 176, 169)) for im in (old_image, new_image))
        if new.name.endswith("_armor.png") and new_image.size != old_image.size:
            new_image = new_image.resize(old_image.size, Image.Resampling.NEAREST)
        entries.extend(((old.stem + " before", old_image), (new.stem + " after", new_image)))
    if entries:
        sheet(filename, entries, scale, columns)


def main():
    new = [(part, machine(SHEET, (x, 0), (x, 1), (x, 2))) for x, part in enumerate(PARTS)]
    references = [
        ("Furnace", machine(VANILLA, (12, 2), (13, 2), (14, 3))),
        ("Dispenser", machine(VANILLA, (14, 2), (13, 2), (14, 3))),
        ("IC2 Compressor", machine(IC2, (5, 3), (5, 4), (5, 1))),
        ("IC2 Macerator", machine(IC2, (3, 3), (3, 4), (3, 1))),
        ("BuildCraft Pump", machine(BC, (3, 6), (3, 6), (4, 6))),
    ]
    sheet("distillation-icons-native.png", new + references, 1, 6)
    sheet("distillation-icons-8x.png", new + references, 8, 6)
    if (BEFORE / "distillation_items.png").is_file():
        old = [(part + " before", sprite(BEFORE / "distillation_items.png", x, 0, 16))
               for x, part in enumerate(PARTS)]
        sheet("distillation-before-after.png", old + new, 8, 6)

    elevation = Image.new("RGBA", (32, 64))
    for x, y, part, row in ((0, 0, 2, 5), (1, 0, 3, 0), (0, 1, 2, 0), (1, 1, 4, 0),
                            (0, 2, 0, 0), (1, 2, 5, 0), (0, 3, 1, 0)):
        elevation.paste(sprite(SHEET, part, row, 16), (x * 16, y * 16))
    sheet("distillation-assembled.png", [("Assembled elevation", elevation)], 6, 1)

    names = ("mask", "oxygen_tank", "flippers")
    parts = ("compressor_grille", "compressor_side", "compressor_cap")
    before_after("underwater-before-after.png", [(n + ".png", "underwater/" + n + ".png")
                 for n in names + parts], 8, 6)
    before_after("underwater-worn-uv.png", [(n + "_armor.png", "underwater/" + n + "_armor.png")
                 for n in names], 3, 2)
    before_after("compressor-gui-before-after.png", [("air_compressor.png", "interfaces/air_compressor.png")], 2, 2)
    for name in ("mask", "oxygen_tank", "flippers"):
        sheet(name + "-icon-1x.png", [(name, image(SOURCE / "underwater" / (name + ".png")))], 1, 1)


if __name__ == "__main__":
    main()
