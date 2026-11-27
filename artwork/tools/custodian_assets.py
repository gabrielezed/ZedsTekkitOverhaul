"""Author the Custodian's classic atlas and original positional cues."""

from math import pi, sin
from pathlib import Path
from random import Random
import struct
import wave

from PIL import Image, ImageDraw

ART = Path(__file__).resolve().parents[1]
SOURCES = ART / "source" / "abyssal"
SOUNDS = ART.parent / "src/minecraft/zed/sounds/custodian"

# UV origin, width, height, depth, material. These match CustodianModel.
PARTS = {
    "head": (0, 0, 14, 10, 12, "skin"),
    "left gill": (56, 0, 2, 5, 5, "gill"),
    "right gill": (70, 0, 2, 5, 5, "gill"),
    "neck": (84, 0, 6, 4, 6, "underside"),
    "torso": (0, 24, 12, 13, 8, "skin"),
    "hips": (42, 25, 10, 5, 8, "underside"),
    "upper arm": (80, 18, 4, 8, 4, "skin"),
    "forearm": (96, 18, 3, 8, 3, "skin"),
    "hand": (110, 18, 3, 3, 3, "underside"),
    "tentacle root": (0, 47, 4, 6, 4, "skin"),
    "tentacle joint": (18, 47, 3, 6, 3, "underside"),
    "tentacle tip": (32, 47, 2, 6, 2, "underside"),
}
MATERIALS = {"skin": (43, 55, 65), "gill": (71, 99, 101), "underside": (46, 62, 68)}

# Duration, fundamental, final frequency ratio, dissonant harmonic, current/noise, attack envelope.
CUES = {
    "ambient": (0.84, 59, 0.74, 1.51, 0.16, 0.27),
    "hurt": (0.24, 112, 0.55, 2.34, 0.23, 0.04),
    "death": (1.16, 88, 0.40, 1.48, 0.22, 0.06),
    "awaken": (0.98, 53, 1.55, 1.50, 0.11, 0.30),
    "phase": (1.21, 67, 1.86, 2.02, 0.14, 0.24),
    "arrival": (0.48, 155, 0.69, 1.68, 0.19, 0.03),
    "strike_charge": (0.72, 110, 1.89, 1.31, 0.15, 0.21),
    "strike_release": (0.27, 137, 0.46, 2.32, 0.38, 0.03),
    "pressure_charge": (1.35, 46, 1.47, 2.01, 0.10, 0.48),
    "pressure_release": (0.69, 62, 0.41, 1.27, 0.29, 0.035),
    "blink_charge": (0.78, 206, 0.58, 1.36, 0.15, 0.45),
    "blink_release": (0.36, 167, 2.09, 2.42, 0.31, 0.04),
    "resonance_charge": (1.06, 82, 1.07, 1.506, 0.06, 0.29),
    "resonance_release": (0.94, 123, 0.90, 1.992, 0.07, 0.06),
}


def faces(u, v, w, h, d):
    return [
        (u, v+d, d, h), (u+d, v+d, w, h), (u+d+w, v+d, d, h),
        (u+2*d+w, v+d, w, h), (u+d, v, w, d), (u+d+w, v, w, d),
    ]


def atlas():
    image = Image.new("RGBA", (128, 64))
    eyes = Image.new("RGBA", image.size)
    random = Random(5091)
    for name, (u, v, w, h, d, material) in PARTS.items():
        for face, (x, y, width, height) in enumerate(faces(u, v, w, h, d)):
            base = MATERIALS[material]
            for i in range(width):
                vein = random.choice((-5, -3, 0, 1, 4, 6))
                for j in range(height):
                    bevel = 4 * (j == 0 or i == 0) - 5 * (j == height - 1 or i == width - 1)
                    grain = random.choice((-3, -1, 0, 0, 1, 2))
                    crease = -15 if material == "gill" and i % 2 == 0 and j > 0 else 0
                    tone = (0, -4, -1, -6, 7, -6)[face] + vein + bevel + grain + crease
                    if name == "head" and face == 1:
                        tone -= 10 if 1 < j < 8 else 3
                    image.putpixel((x+i, y+j), tuple(max(0, c + tone) for c in base) + (255,))
    draw = ImageDraw.Draw(image)
    draw.line((16, 19, 21, 19), fill="#1b202a")
    draw.line((22, 19, 23, 18), fill="#252b37")
    for i in (13, 21):
        draw.rectangle((i, 15, i+3, 17), fill="#252230")
    glow = ImageDraw.Draw(eyes)
    for i in (13, 21):
        glow.line((i, 16, i+3, 16), fill="#9a77bd")
        glow.line((i+1, 16, i+2, 16), fill="#d2b4ed")
    for j in (33, 35, 36, 39):
        draw.point((12+(j % 3), j), fill="#536473")
        draw.point((16+(j % 2), j+1), fill="#3f5260")
    image.save(SOURCES / "custodian.png")
    eyes.save(SOURCES / "custodian_eyes.png")


def audio():
    SOUNDS.mkdir(parents=True, exist_ok=True)
    rate = 22050
    for index, (cue, params) in enumerate(CUES.items()):
        duration, frequency, ratio, harmonic, noise, attack = params
        random = Random(5100 + index)
        phase = low = 0.0
        samples = bytearray()
        for i in range(round(rate * duration)):
            t = i / (rate * duration)
            phase += 2 * pi * frequency * (1 + (ratio-1)*t) / rate
            low = 0.90 * low + 0.10 * random.uniform(-1, 1)
            envelope = min(1, t / attack) * (1-t) ** 1.16
            pulse = 0.86 + 0.14 * sin(2 * pi * t * (2 if "resonance" in cue else 1))
            tone = 0.54 * sin(phase) + 0.17 * sin(harmonic * phase) + 0.09 * sin(2.03 * phase)
            samples.extend(struct.pack("<h", round(24500 * envelope * pulse * (tone + 2.6*noise*low))))
        with wave.open(str(SOUNDS / (cue + ".wav")), "wb") as output:
            output.setparams((1, 2, rate, 0, "NONE", "not compressed"))
            output.writeframes(samples)


if __name__ == "__main__":
    atlas()
    audio()
