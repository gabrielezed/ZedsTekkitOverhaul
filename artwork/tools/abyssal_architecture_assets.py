"""Compose the new architectural marks directly on the existing abyssal masonry pixels."""
from PIL import Image, ImageDraw
from io import BytesIO
from os import environ
from zipfile import ZipFile
from enderic_art import SOURCES, ART

ATLAS = SOURCES / "materials.png"


def cell(image, index):
    x, y = index % 16 * 16, index // 16 * 16
    return image.crop((x, y, x + 16, y + 16))


def main():
    image = Image.open(ATLAS).convert("RGBA")
    brick, eroded = cell(image, 4), cell(image, 5)
    dark = brick.getpixel((1, 7))
    highlight = brick.getpixel((10, 2))
    accents = {}
    for index in (6, 16, 17, 18, 19, 20, 21, 22, 23, 24):
        tile = (eroded if index in (20, 23) else brick).copy()
        draw = ImageDraw.Draw(tile)
        if index == 6:
            draw.rectangle((5, 2, 10, 13), fill=dark, outline=highlight)
            draw.rectangle((7, 4, 8, 11), fill="#b4c9b1")
            draw.line((6, 7, 9, 7), fill="#d8ded0")
        elif index == 16:
            for x in (3, 7, 11):
                draw.line((x, 2, x, 13), fill=dark)
                draw.line((x+1, 3, x+1, 12), fill=highlight)
            draw.line((0, 1, 15, 1), fill=highlight)
            draw.line((0, 14, 15, 14), fill=dark)
        elif index in (17, 22, 24):
            draw.rectangle((3, 3, 12, 12), outline=highlight)
            draw.rectangle((5, 5, 10, 10), outline=dark)
        elif index == 18:
            draw.polygon(((7, 2),(12, 7),(8, 13),(3, 8)), outline=dark)
            draw.line((4, 8, 7, 11, 11, 7), fill=highlight)
        elif index == 19:
            draw.line((0, 3, 15, 3), fill=highlight)
            draw.line((0, 12, 15, 12), fill=dark)
            for x in (1, 9):
                draw.line((x, 9, x+3, 5, x+6, 9), fill=highlight)
        elif index == 20:
            for x in (3, 7, 11):
                draw.rectangle((x, 3, x+1, 12), fill=(0,0,0,0))
        else:
            draw.rectangle((2, 3, 13, 12), outline=highlight)
            draw.line((3, 11, 12, 11), fill=dark)
        image.paste(tile, (index % 16 * 16, index // 16 * 16))
        accents[index] = tile
    image.save(ATLAS)
    names = ["Lamp front", "Pillar", "Pillar cap", "Carved", "Frieze", "Grate", "Pedestal", "Pedestal top", "Lamp rear", "Lamp cap"]
    previous = environ.get("ZTO_REFINEMENT_BASELINE")
    if previous:
        with ZipFile(previous) as archive:
            before = Image.open(BytesIO(archive.read("zed/textures/abyssal/materials.png"))).convert("RGBA")
        accents = {0: cell(before, 6), **accents}
        names.insert(0, "Previous lamp")
    sheet = Image.new("RGB", (640, 125*((len(accents)+4)//5)), "#25313b")
    overlay = ImageDraw.Draw(sheet)
    for i, (index, tile) in enumerate(accents.items()):
        tile = tile.resize((84, 84), Image.Resampling.NEAREST)
        x, y = (i % 5)*128+20, (i // 5)*125+8
        sheet.paste(tile, (x,y), tile)
        overlay.text((x,y+88), names[i], fill="#e0e6ef")
    dest = ART / "previews" / "custodian-refinement"
    dest.mkdir(parents=True, exist_ok=True)
    sheet.save(dest / "architecture-tiles.png")


if __name__ == "__main__":
    main()
