"""Author the Guardian's legacy cuboid UVs and original mono PCM cues."""

from array import array
from math import exp, pi, sin
from pathlib import Path
from random import Random
import sys
import wave

from PIL import Image, ImageDraw


ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "artwork/source/abyssal"
SOUNDS = ROOT / "src/minecraft/zed/sounds/guardian"
PREVIEWS = ROOT / "artwork/previews"
SLATE = ((16, 28, 34), (26, 43, 48), (38, 59, 61), (52, 75, 72), (68, 89, 82))
PLATE = ((21, 30, 35), (36, 49, 51), (52, 66, 66), (70, 83, 78), (91, 100, 86))
FIN = ((11, 22, 29), (17, 35, 43), (23, 50, 58), (32, 66, 70), (44, 79, 78))


def paint(image, rect, palette, bias, seed):
    x, y, width, height = rect
    random = Random(seed)
    for row in range(height):
        for col in range(width):
            tone = bias + random.choice((-1, 0, 0, 0, 1))
            if row == 0:
                tone += 1
            if (col + (row // 2) * 2 + seed) % 7 in (0, 1):
                tone -= 1
            image.putpixel((x + col, y + row), (*palette[max(0, min(tone, 4))], 255))


def cuboid(image, u, v, width, height, length, palette, seed):
    faces = {
        "left": (u, v + length, length, height),
        "face": (u + length, v + length, width, height),
        "right": (u + length + width, v + length, length, height),
        "rear": (u + length * 2 + width, v + length, width, height),
        "top": (u + length, v, width, length),
        "bottom": (u + length + width, v, width, length),
    }
    for index, key in enumerate(faces):
        paint(image, faces[key], palette, (2, 2, 2, 1, 3, 1)[index], seed + index * 19)
    return faces


def texture():
    image = Image.new("RGBA", (64, 64))
    draw = ImageDraw.Draw(image)
    body = cuboid(image, 0, 0, 10, 6, 12, SLATE, 233)
    brow = cuboid(image, 0, 20, 8, 5, 6, PLATE, 91)
    jaw = cuboid(image, 0, 32, 6, 2, 6, PLATE, 97)
    cuboid(image, 34, 20, 5, 4, 7, SLATE, 211)
    cuboid(image, 42, 0, 1, 8, 7, FIN, 318)
    cuboid(image, 48, 31, 1, 5, 6, FIN, 726)
    for u in (0, 29):
        fin = cuboid(image, u, 42, 7, 1, 6, FIN, 119 + u)
        x, y, _, _ = fin["top"]
        for pos in (1, 4):
            draw.line((x + pos, y + 1, x + pos + 2, y + 4), fill=(53, 83, 79, 255))
    for side, position in (("left", 0), ("right", 4)):
        x, y, _, _ = brow[side]
        draw.rectangle((x + position, y + 2, x + position + 1, y + 3), fill=(14, 29, 32, 255))
        draw.point((x + position, y + 2), fill=(145, 203, 185, 255))
        x, y, _, _ = body[side]
        for gill in (3, 5, 7):
            draw.line((x + gill, y + 2, x + gill + 1, y + 4), fill=(13, 24, 30, 255))
    x, y, _, _ = brow["face"]
    draw.line((x + 1, y + 3, x + 6, y + 3), fill=(17, 25, 30, 255))
    draw.point((x + 1, y + 2), fill=(123, 183, 169, 255))
    draw.point((x + 6, y + 2), fill=(123, 183, 169, 255))
    x, y, _, _ = jaw["top"]
    draw.rectangle((x + 1, y + 1, x + 4, y + 4), fill=(25, 22, 27, 255))
    draw.point((x + 1, y + 1), fill=(112, 120, 100, 255))
    draw.point((x + 4, y + 1), fill=(112, 120, 100, 255))
    image.save(SOURCE / "guardian.png")
    PREVIEWS.mkdir(parents=True, exist_ok=True)
    image.resize((512, 512), Image.Resampling.NEAREST).save(PREVIEWS / "guardian-uv.png")


def audio():
    SOUNDS.mkdir(parents=True, exist_ok=True)
    rate = 22050
    cues = {
        "ambient": (0.85, 68, 44, 0.16, 3.0, 0.28),
        "warning": (0.47, 92, 184, 0.28, 14.0, 0.12),
        "lunge": (0.33, 142, 52, 0.31, 9.0, 0.02),
        "hurt": (0.26, 146, 81, 0.24, 28.0, 0.025),
        "death": (0.64, 105, 36, 0.22, 7.0, 0.09),
    }
    for seed, (name, (duration, low, high, gain, flutter, attack)) in enumerate(cues.items(), 510):
        noise = Random(seed)
        smooth, phase = 0.0, 0.0
        pcm = array("h")
        for sample in range(round(rate * duration)):
            time = sample / rate
            position = time / duration
            phase += 2 * pi * (low + (high - low) * position) / rate
            smooth += (noise.uniform(-1, 1) - smooth) * 0.17
            pulse = 0.59 + 0.41 * sin(time * 2 * pi * flutter) ** 2
            envelope = min(1.0, time / attack) * (1 - position) * exp(-0.65 * position)
            tone = sin(phase) * 0.54 + sin(phase * 2.07) * 0.16 + smooth * (0.9 if name == "lunge" else 0.39)
            pcm.append(round(32767 * tone * gain * envelope * pulse))
        if sys.byteorder != "little":
            pcm.byteswap()
        with wave.open(str(SOUNDS / (name + ".wav")), "wb") as stream:
            stream.setparams((1, 2, rate, 0, "NONE", "not compressed"))
            stream.writeframes(pcm.tobytes())


if __name__ == "__main__":
    texture()
    audio()
