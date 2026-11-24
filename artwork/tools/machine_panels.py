"""Draw original machine panels using the Minecraft 1.2.5 and IC2 1.97 GUI palette."""
from PIL import Image, ImageDraw

from textures import ART, save_image

BODY = "#c6c6c6"
SLOT = "#8b8b8b"
INSET = "#373737"
SHADE = "#555555"
LIGHT = "#ffffff"
TEXT = "#404040"


def shell(width, height):
    image = Image.new("RGBA", (256, 256))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((0, 0, width - 1, height - 1), radius=3, fill="#000000")
    draw.rounded_rectangle((1, 1, width - 2, height - 2), radius=2, fill=BODY)
    draw.line((3, 1, width - 4, 1), fill=LIGHT)
    draw.line((2, 2, width - 3, 2), fill=LIGHT)
    draw.line((1, 3, 1, height - 4), fill=LIGHT)
    draw.line((2, 3, 2, height - 3), fill=LIGHT)
    draw.line((width - 2, 3, width - 2, height - 4), fill=SHADE)
    draw.line((width - 3, 3, width - 3, height - 3), fill=SHADE)
    draw.line((3, height - 3, width - 3, height - 3), fill=SHADE)
    draw.line((3, height - 2, width - 4, height - 2), fill=SHADE)
    return image


def recess(draw, x, y, width, height):
    draw.rectangle((x, y, x + width - 1, y + height - 1), fill=LIGHT)
    draw.rectangle((x, y, x + width - 2, y + height - 2), fill=INSET)
    draw.rectangle((x + 1, y + 1, x + width - 2, y + height - 2), fill=SLOT)


def distillation_panel():
    save_image(shell(246, 206), ART / "source/interfaces/distillation.png")


def compressor_panel():
    image = shell(176, 169)
    draw = ImageDraw.Draw(image)
    recess(draw, 15, 25, 18, 18)
    recess(draw, 41, 32, 122, 8)
    for column in range(9):
        x = 7 + 18 * column
        for y in (86, 104, 122, 144):
            recess(draw, x, y, 18, 18)
    draw.polygon(((37, 42), (39, 42), (37, 46), (39, 46), (34, 51),
                  (35, 47), (33, 47)), fill="#887849")
    draw.rectangle((0, 176, 119, 181), fill="#699299")
    draw.line((0, 176, 119, 176), fill="#b0c8c9")
    draw.line((0, 181, 119, 181), fill="#3e626c")
    save_image(image, ART / "source/interfaces/air_compressor.png")


if __name__ == "__main__":
    distillation_panel()
    compressor_panel()
