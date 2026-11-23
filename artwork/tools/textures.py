"""Local reference acquisition, pixel inspection and owned texture export."""
import argparse
from collections import Counter
from hashlib import sha1, sha256
from io import BytesIO
import json
from pathlib import Path, PurePosixPath
import shutil
from urllib.request import urlopen
from zipfile import BadZipFile, ZipFile

from PIL import Image, ImageDraw

ART = Path(__file__).resolve().parents[1]
ROOT = ART.parent
CACHE = ART / "references"
RUNTIME = ROOT / "src" / "minecraft"


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, indent=2) + "\n", encoding="utf-8")


def relative_to(root, name):
    path = PurePosixPath(name)
    if path.is_absolute() or not path.parts or ".." in path.parts or ":" in name or "\\" in name:
        raise ValueError(f"Unsafe relative path: {name}")
    target = (root / path).resolve()
    if not target.is_relative_to(root.resolve()):
        raise ValueError(f"Path escapes {root}: {name}")
    return target


def verified(data, checksum, algorithm, name):
    if algorithm(data).hexdigest() != checksum:
        raise ValueError(f"Checksum mismatch: {name}")
    return data


def download(url):
    with urlopen(url, timeout=45) as response:
        return response.read()


def import_images(source, destination, prefixes, source_info):
    def selected(name):
        return name.endswith(".png") and any(
            name.startswith(prefix) if prefix.endswith("/") else name == prefix
            for prefix in prefixes
        )

    records = {}
    if source.is_dir():
        for file in sorted(source.rglob("*.png")):
            name = file.relative_to(source).as_posix()
            if selected(name):
                records[name] = file.read_bytes()
    else:
        archive_bytes = source.read_bytes()
        source_info = dict(source_info, archiveSha256=sha256(archive_bytes).hexdigest())
        with ZipFile(BytesIO(archive_bytes)) as archive:
            records = {name: archive.read(name) for name in sorted(archive.namelist()) if selected(name)}
    if not records and not source_info.get("sharedImages"):
        raise ValueError(f"No matching 1.2.5 resources in {source}")
    index = {}
    for name, data in records.items():
        output = relative_to(destination, name)
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_bytes(data)
        index[name] = sha256(data).hexdigest()
    write_json(destination / "index.json", dict(source_info, resources=index))
    print(f"{destination.name}: {len(index)} images")


def vanilla(args):
    spec = read_json(ART / "manifests/reference-sources.json")["vanilla"]
    source = args.archive or CACHE / "downloads" / "minecraft-1.2.5.jar"
    if not source.exists() and not args.archive:
        try:
            versions = json.loads(download(spec["manifest"]))["versions"]
            version = next(entry for entry in versions if entry["id"] == spec["version"])
            metadata = json.loads(verified(download(version["url"]), version["sha1"], sha1, version["url"]))
            client = metadata["downloads"]["client"]
            if client["sha1"] != spec["sha1"]:
                raise ValueError("Published Minecraft 1.2.5 client checksum differs from the pinned checksum")
            data = verified(download(client["url"]), spec["sha1"], sha1, client["url"])
        except (OSError, ValueError, KeyError, StopIteration) as error:
            raise ValueError(f"Vanilla acquisition failed: {error}. Use vanilla --archive with the official 1.2.5 client.") from error
        source.parent.mkdir(parents=True, exist_ok=True)
        source.write_bytes(data)
    verified(source.read_bytes(), spec["sha1"], sha1, str(source))
    import_images(source, CACHE / "vanilla", spec["resources"],
                  {"version": spec["version"], "archive": source.name, "publishedSha1": spec["sha1"],
                   "versionManifest": spec["manifest"]})


def mods(args):
    specs = read_json(ART / "manifests/reference-sources.json")["mods"]
    catalog = ROOT / "tekkit-mods-decompiles" / "mods"
    search = args.mods_dir or Path.home() / "AppData/Roaming/PrismLauncher/instances/Tekkit Dev/minecraft/mods"
    for name, spec in specs.items():
        archive = spec["archive"]
        source = search / archive
        if not source.is_file():
            source = catalog / archive / "extracted"
        if not source.exists():
            raise ValueError(f"{archive} missing. Supply --mods-dir with the installed Tekkit Classic 3.1.2 archives "
                             "or restore the local tekkit-mods-decompiles catalog.")
        source_info = {"archive": archive, "sourceType": "extracted" if source.is_dir() else "archive"}
        if "sharedImages" in spec:
            source_info["sharedImages"] = spec["sharedImages"]
        import_images(source, CACHE / name, spec["resources"], source_info)


def cells(image, size):
    if size <= 0 or image.width % size or image.height % size:
        raise ValueError(f"{image.size} is not a grid of {size}-pixel cells")
    for y in range(image.height // size):
        for x in range(image.width // size):
            yield x, y, image.crop((x * size, y * size, (x + 1) * size, (y + 1) * size))


def sprite(path, x, y, size):
    with Image.open(path) as image:
        for column, row, cell in cells(image.convert("RGBA"), size):
            if (x, y) == (column, row):
                return cell
    raise ValueError(f"No cell {x},{y} in {path}")


def artwork_sheet(entries, scale, columns):
    if not entries or scale < 1 or columns < 1:
        raise ValueError("A sheet requires at least one image, a positive scale and positive columns")
    columns = min(columns, len(entries))
    cell_width = max(max(im.width * scale, len(label) * 7) for label, im in entries) + 12
    cell_height = max(im.height * scale for _, im in entries) + 25
    sheet = Image.new("RGB", (cell_width * columns, cell_height * ((len(entries) + columns - 1) // columns)), "#292b2b")
    draw = ImageDraw.Draw(sheet)
    for index, (label, im) in enumerate(entries):
        left = (index % columns) * cell_width + 6
        top = (index // columns) * cell_height + 5
        enlarged = im.convert("RGBA").resize((im.width * scale, im.height * scale), Image.Resampling.NEAREST)
        for y in range(im.height):
            for x in range(im.width):
                if im.getpixel((x, y))[3] < 255:
                    draw.rectangle((left + x * scale, top + y * scale, left + (x + 1) * scale - 1,
                                    top + (y + 1) * scale - 1), fill="#555555" if (x + y) % 2 else "#414141")
        sheet.paste(enlarged, (left, top), enlarged)
        draw.text((left, top + enlarged.height + 3), label, fill="#ffffff")
    return sheet


def build_atlas(layout, directory):
    size = layout["cell"]
    canvas = Image.new("RGBA", tuple(layout["size"]))
    used = set()
    for name, x, y in layout["sprites"]:
        if (x, y) in used or not (0 <= x * size <= canvas.width - size and 0 <= y * size <= canvas.height - size):
            raise ValueError(f"Invalid or duplicate cell {x},{y}")
        with Image.open(relative_to(directory, name)) as image:
            if image.size != (size, size):
                raise ValueError(f"{name} is not {size}x{size}")
            canvas.paste(image.convert("RGBA"), (x * size, y * size))
        used.add((x, y))
    return canvas


def inspect(args):
    with Image.open(args.image) as image:
        rgba = image.convert("RGBA")
        print(f"{args.image}: {image.width}x{image.height}, {image.mode}")
        for x, y, cell in cells(rgba, args.cell):
            alpha = cell.getchannel("A")
            if alpha.getbbox():
                print(f"{x:2},{y:2}  index {y * (image.width // args.cell) + x:3}  alpha {alpha.getextrema()}")


def save_image(image, output):
    output.parent.mkdir(parents=True, exist_ok=True)
    image.save(output, format="PNG", compress_level=9)


def preview(args):
    entries = []
    for value in args.images:
        path, at, coords = value.rpartition("@")
        if at:
            x, y = map(int, coords.split(","))
            image = sprite(Path(path), x, y, args.cell)
            label = f"{Path(path).stem} {x},{y}"
        else:
            with Image.open(value) as image_file:
                image = image_file.convert("RGBA")
            label = Path(value).stem
        if args.atlas:
            entries.extend((f"{label} {x},{y}", cell) for x, y, cell in cells(image, args.cell))
        else:
            entries.append((label, image))
    save_image(artwork_sheet(entries, args.scale, args.columns), args.output)


def palette(args):
    counts = Counter(pixel[:3] for value in args.images
                     for pixel in palette_image(value, args.cell).get_flattened_data() if pixel[3] == 255)
    result = {"samples": args.images,
              "colors": [("#%02x%02x%02x" % color, count) for color, count in counts.most_common(args.limit)]}
    write_json(args.output, result)


def palette_image(value, size):
    path, at, coords = value.rpartition("@")
    if at:
        x, y = map(int, coords.split(","))
        return sprite(Path(path), x, y, size)
    with Image.open(value) as image:
        return image.convert("RGBA")


def validate_assets(manifest, base=ART, runtime=RUNTIME, export=False, jar=None):
    targets = set()
    ids = set()
    approved = []
    for entry in manifest:
        identifier = entry["id"]
        target = entry["runtime"]
        if (identifier in ids or target.casefold() in targets or not entry["jar"]
                or not target.startswith(("zed/textures/", "zed/gui/")) or not target.endswith(".png")
                or not entry["source"].startswith("source/")):
            raise ValueError(f"Invalid or duplicate asset mapping: {identifier} -> {target}")
        if entry["origin"] not in ("zto-original", "zto-reference-inspired", "licensed-derivative", "local-derivative"):
            raise ValueError(f"Unapproved artwork origin for {identifier}")
        if entry["origin"] == "licensed-derivative" and not entry.get("permission"):
            raise ValueError(f"Missing derivative permission for {identifier}")
        if entry["origin"] == "local-derivative" and not relative_to(base, entry.get("derivation", "")).is_file():
            raise ValueError(f"Missing local derivation record for {identifier}")
        if not entry["role"] or not entry["palette"]:
            raise ValueError(f"Missing asset description for {identifier}")
        ids.add(identifier)
        targets.add(target.casefold())
        source = relative_to(base, entry["source"])
        destination = relative_to(runtime, target)
        if source == destination:
            raise ValueError(f"Source and runtime must be distinct: {identifier}")
        with Image.open(source) as img:
            if img.format != "PNG" or img.size != tuple(entry["size"]) or img.mode not in ("RGB", "RGBA"):
                raise ValueError(f"Wrong PNG format or dimensions: {source}")
            image = img.convert("RGBA")
        alpha = image.getchannel("A")
        policy = entry["alpha"]
        values = set(alpha.get_flattened_data())
        if (policy == "opaque" and values != {255}
                or policy == "cutout" and values != {0, 255}
                or policy not in ("opaque", "cutout")):
            raise ValueError(f"Unexpected alpha values in {source}")
        for x, y in entry.get("opaqueCells", []):
            if not (0 <= x < image.width // 16 and 0 <= y < image.height // 16):
                raise ValueError(f"Atlas cell {x},{y} is out of bounds: {source}")
            if alpha.crop((16*x, 16*y, 16*x+16, 16*y+16)).getextrema() != (255, 255):
                raise ValueError(f"Atlas cell {x},{y} is missing pixels: {source}")
        if not export and (not destination.is_file() or source.read_bytes() != destination.read_bytes()):
            raise ValueError(f"Missing or stale exported asset: {target}. Run export.")
        if jar and jar.read(target) != source.read_bytes():
            raise ValueError(f"Missing or stale packaged asset: {target}")
        approved.append((source, destination))
    if export:
        for source, destination in approved:
            destination.parent.mkdir(parents=True, exist_ok=True)
            shutil.copyfile(source, destination)
    return len(approved)


def assets(args):
    manifest = read_json(ART / "manifests/assets.json")
    if getattr(args, "jar", None):
        with ZipFile(args.jar) as jar:
            count = validate_assets(manifest, jar=jar)
    else:
        count = validate_assets(manifest, export=args.command == "export")
    print(f"{args.command}: {count} assets")


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    commands = parser.add_subparsers(dest="command", required=True)
    command = commands.add_parser("vanilla")
    command.add_argument("--archive", type=Path)
    command.set_defaults(run=vanilla)
    command = commands.add_parser("mods")
    command.add_argument("--mods-dir", type=Path)
    command.set_defaults(run=mods)
    command = commands.add_parser("inspect")
    command.add_argument("image", type=Path)
    command.add_argument("--cell", type=int, default=16)
    command.set_defaults(run=inspect)
    command = commands.add_parser("extract")
    command.add_argument("image", type=Path)
    command.add_argument("x", type=int)
    command.add_argument("y", type=int)
    command.add_argument("output", type=Path)
    command.add_argument("--cell", type=int, default=16)
    command.set_defaults(run=lambda a: save_image(sprite(a.image, a.x, a.y, a.cell), a.output))
    command = commands.add_parser("build-atlas")
    command.add_argument("layout", type=Path)
    command.add_argument("output", type=Path)
    command.set_defaults(run=lambda a: save_image(build_atlas(read_json(a.layout), a.layout.parent), a.output))
    for name, action in (("preview", preview), ("palette", palette)):
        command = commands.add_parser(name)
        command.add_argument("output", type=Path)
        command.add_argument("images", nargs="+")
        command.add_argument("--cell", type=int, default=16)
        if name == "preview":
            command.add_argument("--atlas", action="store_true")
            command.add_argument("--scale", type=int, default=2)
            command.add_argument("--columns", type=int, default=8)
        else:
            command.add_argument("--limit", type=int, default=12)
        command.set_defaults(run=action)
    commands.add_parser("export").set_defaults(run=assets)
    command = commands.add_parser("validate")
    command.add_argument("--jar", type=Path)
    command.set_defaults(run=assets)
    args = parser.parse_args()
    try:
        args.run(args)
    except (OSError, ValueError, KeyError, BadZipFile) as error:
        parser.exit(1, f"{args.command}: {error}\n")


if __name__ == "__main__":
    main()
