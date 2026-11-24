"""Author original refinery indicator cells, panel skin and mechanical WAVs."""
import math
import random
import struct
import wave

from PIL import Image, ImageDraw

from textures import ART, RUNTIME, save_image


def panel():
    image = Image.new("RGBA", (256, 256))
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, 245, 205), fill="#353532")
    draw.rectangle((1, 1, 244, 204), fill="#706e67")
    draw.rectangle((1, 1, 243, 203), fill="#dedbd0")
    draw.rectangle((3, 3, 242, 202), fill="#bcb9ae")
    draw.rectangle((9, 28, 236, 171), fill="#858278")
    draw.rectangle((10, 29, 236, 171), fill="#e0ddd1")
    draw.rectangle((11, 30, 235, 170), fill="#c9c6bb")
    draw.rectangle((11, 30, 235, 43), fill="#b4b2a7")
    draw.line((11, 43, 235, 43), fill="#9e9a8e")
    draw.line((10, 22, 235, 22), fill="#878479")
    draw.line((10, 23, 235, 23), fill="#d8d5c9")
    for x, y in ((6, 6), (238, 6), (6, 198), (238, 198)):
        draw.rectangle((x, y, x + 2, y + 2), fill="#777469")
        draw.point((x + 2, y + 2), fill="#e0ddd1")
    save_image(image, ART / "source/interfaces/distillation.png")


def indicators():
    image = Image.new("RGBA", (256, 256))

    def cell(frame, row, color="#242c29"):
        tile = Image.new("RGBA", (16, 16), "#464747")
        draw = ImageDraw.Draw(tile)
        draw.rectangle((1, 1, 14, 14), fill=color)
        draw.line((1, 15, 15, 15), fill="#96938a")
        image.paste(tile, (16 * frame, 16 * row))
        return ImageDraw.Draw(image)

    for frame, heat in enumerate(("#9d5b34", "#bf743f", "#d39454", "#bb703b")):
        x = 16 * frame
        draw = cell(frame, 0, "#271d19")
        for mark in range(3, 14, 3):
            draw.rectangle((x + mark, 4, x + mark + 1, 11), fill=heat)
        draw = cell(frame, 1, "#252822")
        draw.line((x + 3, 18, x + 3, 29), fill="#625e49")
        for mark in (0, 7):
            y = 18 + (mark + 12 - frame * 3) % 12
            draw.line((x + 5, y, x + 11, y), fill="#ab9669", width=1)
        draw = cell(frame, 2, "#222d34")
        draw.rectangle((x + 2, 41, x + 13, 45), fill="#416271")
        streak = x + 2 + 2 * frame
        draw.line((streak, 40, streak + 5, 40), fill="#83aebb")

    for frame in range(11):
        x = 16 * frame
        draw = cell(frame, 3, "#242a25")
        if frame <= 8 and frame:
            y = 62 - round(frame * 12 / 8)
            draw.rectangle((x + 3, y, x + 12, 61), fill="#758758")
            draw.line((x + 3, y, x + 12, y), fill="#a5aa76")
        elif frame in (9, 10):
            draw.rectangle((x + 4, 53, x + 11, 58), fill="#b76a49" if frame == 9 else "#b6be77")
        draw.line((x + 2, 50, x + 2, 61), fill="#667163")

    for frame in range(6):
        x = 16 * frame
        draw = cell(frame, 4, "#282b27")
        if frame == 5:
            draw.rectangle((x + 6, 69, x + 9, 75), fill="#b37c52")
        else:
            draw.rectangle((x + 2, 69, x + 3 + 3 * frame, 75), fill="#839365")
    save_image(image, ART / "source/distillation/distillation_indicators.png")


def synthesize_sounds():
    output = RUNTIME / "zed/sounds/distillation"
    output.mkdir(parents=True, exist_ok=True)
    rate = 22050
    clips = (
        ("start", .15, ((0.00, 143, .38, 29), (.040, 367, .24, 66))),
        ("running", .23, ((0.00, 117, .32, 11), (.062, 180, .16, 21))),
        ("complete", .26, ((0.00, 285, .31, 29), (.038, 421, .18, 24), (.082, 213, .12, 20))),
    )
    for name, duration, resonances in clips:
        noise = random.Random(name)
        filtered = 0
        data = bytearray()
        for sample in range(round(duration * rate)):
            t = sample / rate
            filtered = .84 * filtered + .16 * noise.uniform(-1, 1)
            envelope = min(t / .008, 1, (duration - t) / .028)
            level = filtered * (.17 if name == "complete" else .07) * math.exp(-12 * t)
            for start, frequency, gain, decay in resonances:
                age = t - start
                if age >= 0:
                    attack = min(1, age / .006)
                    level += gain * attack * math.sin(math.tau * frequency * age) * math.exp(-decay * age)
            data.extend(struct.pack("<h", round(level * envelope * 32767)))
        with wave.open(str(output / (name + ".wav")), "wb") as clip:
            clip.setparams((1, 2, rate, 0, "NONE", "not compressed"))
            clip.writeframes(data)


if __name__ == "__main__":
    panel()
    indicators()
    synthesize_sounds()
