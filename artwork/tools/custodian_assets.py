"""Author the Custodian's classic atlas and original positional cues."""

from math import pi, sin
from pathlib import Path
from random import Random
import struct
import wave

from PIL import Image, ImageDraw
from enderic_art import (
    ARM, HAND, LEG, SABOTEUR_HEAD, SABOTEUR_TORSO, THIEF_HEAD, THIEF_TORSO,
    donor, embellish_gills, faces, remap,
)

ART = Path(__file__).resolve().parents[1]
SOURCES = ART / "source" / "abyssal"
SOUNDS = ART.parent / "src/minecraft/zed/sounds/custodian"

# UV origin, width, height, depth, material. These match CustodianModel.
PARTS = {
    "head": (0, 0, 14, 12, 16),
    "left gill": (62, 0, 3, 8, 6),
    "right gill": (81, 0, 3, 8, 6),
    "neck": (102, 0, 6, 8, 6),
    "torso": (0, 29, 14, 22, 10),
    "mantle": (103, 30, 4, 12, 8),
    "waist": (58, 29, 12, 7, 10),
    "upper arm": (0, 65, 5, 13, 5),
    "forearm": (22, 65, 4, 12, 4),
    "hand": (40, 65, 3, 5, 3),
    "tentacle root": (0, 90, 6, 12, 5),
    "tentacle joint": (23, 90, 4, 11, 4),
    "tentacle taper": (41, 90, 3, 10, 3),
    "tentacle tip": (55, 90, 2, 9, 2),
}

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
    "compression_charge": (1.22, 112, 0.30, 1.338, 0.23, 0.38),
    "compression_release": (1.04, 48, 2.00, 1.438, 0.16, 0.19),
}


def atlas():
    image = Image.new("RGBA", (128, 128))
    eyes = Image.new("RGBA", image.size)
    thief, saboteur = donor("thief"), donor("saboteur")
    for part, uv in PARTS.items():
        if part == "head":
            source, region = thief, THIEF_HEAD
        elif part in ("neck", "waist", "mantle"):
            source, region = saboteur, SABOTEUR_TORSO
        elif part == "torso":
            source, region = thief, THIEF_TORSO
        elif part == "hand":
            source, region = thief, HAND
        elif part.startswith("tentacle"):
            source, region = thief, LEG
        else:
            source, region = thief, ARM
        remap(image, source, region, uv)
        if "gill" in part:
            embellish_gills(image, uv)
    remap(eyes, donor("thief_eyes"), THIEF_HEAD, PARTS["head"])
    # Pale ribs follow the donor's violet-gray shading without replacing the skin.
    draw = ImageDraw.Draw(image)
    x, y, _, _ = faces(*PARTS["torso"])[1]
    for i in range(5):
        r = 5 + i * 3
        for side in (-1, 1):
            draw.line((x + 6 + side, y + r, x + 6 + side * (3 + i // 2), y + r - 2), fill="#363542")
            draw.point((x + 6 + side * (3 + i // 2), y + r - 2), fill="#494653")
    for face in (0, 2):
        x, y, w, h = faces(*PARTS["mantle"])[face]
        for rib in range(3):
            draw.line((x+1, y+2+rib*3, x+w-2, y+1+rib*3), fill="#3b3b48")
            draw.line((x+2, y+3+rib*3, x+w-2, y+2+rib*3), fill="#232431")
    for part in ("tentacle root", "tentacle joint", "tentacle taper", "tentacle tip"):
        x, y, w, h = faces(*PARTS[part])[1]
        for row in range(1, h - 1, 3):
            draw.point((x + w // 2, y + row), fill="#494653")
            if w >= 4:
                draw.point((x + w // 2 - 1, y + row + 1), fill="#25232e")
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
