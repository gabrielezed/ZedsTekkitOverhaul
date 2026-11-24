"""Author the pixel-aligned Motorboat motor, icon overlays and original sound cue."""
import math
import struct
import wave

from PIL import Image, ImageColor, ImageDraw

from textures import ART, save_image

SOURCE = ART / "source/motorboat"
PALETTE = {
    "h": "#e3e2de",  # BuildCraft bright iron, base_iron (20, 4)
    "l": "#c4c6c6",  # BuildCraft bevel, base_iron (0, 17)
    "m": "#a1aaae",  # IC2 casing, block_machine (94, 56)
    "s": "#858d92",  # IC2 shaded casing, block_machine (95, 62)
    "d": "#464b4e",  # IC2 mechanical panel, block_machine (82, 58)
    "v": "#302e2a",  # BuildCraft dark border, base_iron (21, 12)
    "x": "#35383a",
    "b": "#8d633e",
    "B": "#b48b5d",
}


def pixels(image, x, y, rows):
    for row, line in enumerate(rows):
        for col, pixel in enumerate(line):
            if pixel != ".":
                image.putpixel((x + col, y + row), ImageColor.getcolor(PALETTE[pixel], "RGBA"))


def icons():
    outboard = Image.new("RGBA", (16, 16))
    pixels(outboard, 3, 1, (
        "..xxxx...", ".xhhhxx..", "xhlllmxx.", "xlmmmsdx.", "xlbmsddx.",
        "xlmsddx..", ".xddxx...", "..xsdx...", "..xsdx...", "..xsdx...",
        "..xsdx...", ".xxsmxxx.", ".xdssvdx.", "..xxxxx..",
    ))
    save_image(outboard, SOURCE / "outboard.png")

    overlay = Image.new("RGBA", (16, 16))
    pixels(overlay, 10, 0, (
        "..xxx.", ".xhlxx", "xhlsdx", "xlBsdx", "xmsddx",
        ".xxxx.", "...xdx", "...xdx", "...xdx", "....xx",
    ))
    save_image(overlay, SOURCE / "motorboat_overlay.png")


def cuboid(image, u, v, w, h, d, colors):
    draw = ImageDraw.Draw(image)
    faces = ((u + d, v, w, d), (u + d + w, v, w, d), (u, v + d, d, h),
             (u + d, v + d, w, h), (u + d + w, v + d, d, h),
             (u + 2 * d + w, v + d, w, h))
    for (x, y, width, height), color in zip(faces, colors):
        draw.rectangle((x, y, x + width - 1, y + height - 1), fill=PALETTE[color])


def engine():
    image = Image.new("RGBA", (64, 32))
    cuboid(image, 0, 0, 6, 5, 6, "hdsmsm")
    pixels(image, 6, 0, ("lllllh", "lmmmml", "lmmmmh", "lmmmmh", "lmmmml", "lllllh"))
    pixels(image, 0, 6, ("hlllls", "lmddms", "lmvvms", "lsddss", "dddddd"))
    pixels(image, 12, 6, ("llllll", "msvvsm", "msddsm", "msvvsm", "dddddd"))
    for u in (6, 18):
        pixels(image, u, 6, ("hlllll", "lmmmms", "lmBBms", "lmssms", "dddddd"))
    cuboid(image, 0, 13, 5, 1, 4, "lsmmmm")
    pixels(image, 4, 13, ("hhlll", "lmmml", "lmmml", "lllll"))
    cuboid(image, 24, 0, 5, 2, 4, "sdmdmd")
    cuboid(image, 44, 0, 2, 8, 2, "mssdmd")
    cuboid(image, 24, 8, 4, 2, 2, "msdddd")
    cuboid(image, 36, 10, 1, 4, 2, "sdmsds")
    cuboid(image, 44, 12, 1, 2, 4, "sdmsds")
    save_image(image, SOURCE / "engine.png")


def motor_sound():
    output = ART.parent / "src/minecraft/zed/sounds/motorboat/running.wav"
    output.parent.mkdir(parents=True, exist_ok=True)
    rate = 22050
    duration = 0.36
    samples = []
    for sample in range(int(rate * duration)):
        t = sample / rate
        phase = 2 * math.pi * (67 * t + 0.13 * math.sin(2 * math.pi * 3.8 * t))
        combustion = (0.45 * math.sin(phase) + 0.30 * math.sin(phase * 2)
                      + 0.15 * math.sin(phase * 3) + 0.10 * math.sin(phase * 5))
        envelope = min(1, t / 0.018, (duration - t) / 0.054)
        samples.append(int(6800 * envelope * combustion))
    with wave.open(str(output), "wb") as clip:
        clip.setparams((1, 2, rate, len(samples), "NONE", "not compressed"))
        clip.writeframes(struct.pack(f"<{len(samples)}h", *samples))


if __name__ == "__main__":
    icons()
    engine()
    motor_sound()
