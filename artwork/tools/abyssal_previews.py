"""Preview the authored abyssal tiles and a hand-arranged underwater elevation."""
from PIL import Image, ImageDraw, ImageEnhance

from alpha1_previews import cube, sheet
from textures import ART, save_image, sprite

BLOCKS = ART / "source/abyssal/materials.png"
ITEMS = ART / "source/abyssal/items.png"
SOLIDS = (
    "Abyssal Stone", "Abyssal Silt", "Shellstone", "Petrified Coral",
    "Abyssal Bricks", "Eroded Abyssal Bricks", "Abyssal Lamp",
)
PLANTS = (
    ("Kelp stem", 7, 16), ("Kelp tip", 8, 16), ("Ribbon Algae", 9, 9),
    ("Deepwater Fan Coral", 10, 14), ("Tube Sponge", 14, 11), ("Ghost Anemone", 13, 8),
)
RESOURCES = (
    "Kelp Frond", "Ribbon Algae Cutting", "Fan Coral Fragment",
    "Sponge Cutting", "Luminous Polyp", "Abyssal Shell",
)


def tile(cell):
    return sprite(BLOCKS, cell, 0, 16)


def plant(cell, height):
    shape = Image.new("RGBA", (16, 16))
    shape.paste(tile(cell).resize((16, height), Image.Resampling.NEAREST), (0, 16 - height))
    return shape


def sponge():
    shape = Image.new("RGBA", (16, 16))
    for x, y, width, height in ((3, 4, 5, 11), (8, 8, 4, 7), (6, 10, 5, 5)):
        shape.paste(tile(11).resize((width, height), Image.Resampling.NEAREST), (x, y))
        shape.paste(tile(12).resize((width, 3), Image.Resampling.NEAREST), (x, y - 1))
    return shape


def mockup():
    width, height = 23, 10
    scene = Image.new("RGBA", (width * 16, height * 16))
    draw = ImageDraw.Draw(scene)
    for y in range(scene.height):
        draw.line((0, y, scene.width, y), fill=(17-y//40, 43-y//15, 53-y//12, 255))

    def draw_block(cell, x, y, brightness=0.85):
        source = ImageEnhance.Brightness(tile(cell)).enhance(brightness)
        scene.paste(source, (x * 16, y * 16))

    for x in range(width):
        for y in range(8, 10):
            draw_block(1 if y == 8 else 0, x, y, 0.80)
    for x, top in enumerate((2, 2, 3, 4, 4, 5, 6)):
        for y in range(top, 8):
            draw_block(2 if (x * 5 + y) % 7 == 0 else 0, x, y, 0.71 + (x % 3) / 20)
    for x, top in ((11, 7), (12, 5), (13, 6), (14, 7)):
        for y in range(top, 8):
            draw_block(3, x, y)
    for x, top in ((17, 6), (18, 4), (19, 4), (20, 5), (21, 7)):
        for y in range(top, 8):
            draw_block(6 if (x,y) == (18,6) else 4 + ((x + 2*y) % 3 == 0), x, y)

    for x, length in ((7, 4), (9, 3), (15, 2)):
        for offset in range(length):
            texture = tile(8 if offset == length - 1 else 7)
            scene.paste(texture, (16*x, 16*(7-offset)), texture)
    for x, y, cell, height in ((8,7,9,9), (10,7,10,14), (13,5,9,9), (16,7,13,8), (22,7,14,11)):
        shape = sponge() if cell == 14 else plant(cell, height)
        scene.paste(shape, (16*x,16*y), shape)
    return scene.resize((scene.width * 3, scene.height * 3), Image.Resampling.NEAREST)


def main():
    solids = [(name, tile(index)) for index, name in enumerate(SOLIDS)]
    marine = [(name, sponge() if cell == 14 else plant(cell, height))
              for name, cell, height in PLANTS]
    items = [(name, sprite(ITEMS, index, 0, 16)) for index, name in enumerate(RESOURCES)]
    sheet("abyssal-materials-native.png", solids, 1, 7)
    sheet("abyssal-cubes.png", [(name, cube(face, face, face)) for name, face in solids], 6, 7)
    sheet("abyssal-marine.png", marine, 6, 6)
    sheet("abyssal-items.png", items, 6, 6)
    sheet("abyssal-sprites-enlarged.png",
          solids + [(name, tile(cell)) for name, cell, _ in PLANTS] + items, 8, 5)
    save_image(mockup(), ART / "previews/abyssal-underwater-mockup.png")


if __name__ == "__main__":
    main()
