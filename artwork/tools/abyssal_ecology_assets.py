"""Author only the diver equipment, Abyssal Enderkin UVs and its original PCM cues."""

from array import array
from math import exp, pi, sin
from random import Random
import sys
import wave

from PIL import Image, ImageDraw

from guardian_assets import PREVIEWS, ROOT, SOURCE, cuboid
from enderic_art import enderkin

RUBBER = ((16, 20, 23), (26, 34, 38), (39, 51, 50), (57, 69, 65), (77, 88, 77))
LENS = ((25, 53, 56), (44, 83, 82), (59, 107, 105), (95, 145, 132), (149, 186, 163))
METAL = ((28, 38, 44), (43, 59, 64), (64, 78, 78), (90, 104, 97), (119, 129, 112))


def gear():
    image = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(image)
    cuboid(image, 0, 0, 8, 3, 2, RUBBER, 14)
    cuboid(image, 24, 0, 3, 2, 1, LENS, 22)
    cuboid(image, 0, 8, 3, 2, 2, METAL, 73)
    tube = cuboid(image, 12, 8, 1, 6, 1, RUBBER, 87)
    for x, y, width, height in (tube[face] for face in ("face", "left", "right", "rear")):
        for row in range(0, height, 2):
            draw.line((x, y + row, x + width - 1, y + row), fill=(73, 82, 73, 255))
    cuboid(image, 42, 16, 2, 2, 2, METAL, 611)
    cylinder = cuboid(image, 0, 16, 5, 9, 4, METAL, 342)
    for face in ("left", "right", "face", "rear"):
        x, y, width, height = cylinder[face]
        for offset in (2, 6):
            draw.line((x, y + offset, x + width - 1, y + offset), fill=(35, 47, 48, 255))
    straps = cuboid(image, 22, 16, 1, 9, 5, RUBBER, 259)
    for face in ("face", "rear"):
        x, y, _, _ = straps[face]
        draw.point((x, y + 3), fill=(124, 112, 73, 255))
    fins = cuboid(image, 0, 34, 4, 1, 6, RUBBER, 446)
    x, y, _, _ = fins["top"]
    for ridge in (0, 3):
        draw.line((x + ridge, y + 1, x + ridge, y + 4), fill=(74, 86, 75, 255))
    return image


def sounds():
    target = ROOT / "src/minecraft/zed/sounds/abyssalenderkin"
    target.mkdir(parents=True, exist_ok=True)
    cues = {
        "ambient": (0.73, 145, 125, 0.19, 5),
        "charge": (0.83, 130, 365, 0.28, 11),
        "release": (0.33, 325, 107, 0.30, 19),
        "hurt": (0.28, 208, 136, 0.25, 13),
        "death": (0.59, 151, 64, 0.24, 8),
    }
    for seed, (name, (duration, start, finish, volume, tremolo)) in enumerate(cues.items(), 1001):
        pcm = array("h")
        random = Random(seed)
        phase = noise = 0.0
        for sample in range(round(22050 * duration)):
            t = sample / 22050
            unit = t / duration
            phase += 2 * pi * (start + (finish - start) * unit) / 22050
            noise += (random.uniform(-1, 1) - noise) * 0.14
            envelope = min(1.0, t * (6 if name == "charge" else 55)) * (1 - unit) ** 0.6
            signal = 0.57 * sin(phase + sin(2 * pi * t * 26) * 0.8) + 0.23 * sin(2.03 * phase) + noise * 0.4
            signal *= (0.75 + 0.25 * sin(2 * pi * t * tremolo)) * exp(-0.21 * unit)
            pcm.append(round(signal * envelope * volume * 32767))
        if sys.byteorder != "little":
            pcm.byteswap()
        with wave.open(str(target / (name + ".wav")), "wb") as stream:
            stream.setparams((1, 2, 22050, 0, "NONE", "not compressed"))
            stream.writeframes(pcm.tobytes())


if __name__ == "__main__":
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    skin, eyes = enderkin()
    for name, image in (("diver_equipment", gear()), ("enderkin", skin), ("enderkin_eyes", eyes)):
        image.save(SOURCE / (name + ".png"))
        image.resize((384, 384), Image.Resampling.NEAREST).save(PREVIEWS / ("abyssal-" + name + "-uv.png"))
    sounds()
