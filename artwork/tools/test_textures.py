from hashlib import sha1
from pathlib import Path
from tempfile import TemporaryDirectory
import unittest
from zipfile import ZipFile

from PIL import Image

import textures


class TextureTests(unittest.TestCase):
    def setUp(self):
        folder = TemporaryDirectory()
        self.addCleanup(folder.cleanup)
        self.root = Path(folder.name)

    def test_atlas_assembly_extraction_and_preview(self):
        left = Image.new("RGBA", (16, 16), (210, 23, 11, 255))
        right = Image.new("RGBA", (16, 16))
        right.putpixel((15, 0), (18, 202, 42, 255))
        left.save(self.root / "left.png")
        right.save(self.root / "right.png")
        layout = {"cell": 16, "size": [32, 16], "sprites": [["right.png", 1, 0], ["left.png", 0, 0]]}
        atlas = textures.build_atlas(layout, self.root)
        output = self.root / "atlas.png"
        textures.save_image(atlas, output)
        first = output.read_bytes()
        textures.save_image(textures.build_atlas(layout, self.root), output)
        self.assertEqual(output.read_bytes(), first)
        self.assertEqual(textures.sprite(output, 0, 0, 16).tobytes(), left.tobytes())
        cell = textures.sprite(output, 1, 0, 16)
        self.assertEqual(cell.tobytes(), right.tobytes())
        self.assertEqual(cell.getpixel((0, 0)), (0, 0, 0, 0))
        with self.assertRaisesRegex(ValueError, "No cell"):
            textures.sprite(output, 2, 0, 16)
        with self.assertRaisesRegex(ValueError, "duplicate cell"):
            textures.build_atlas(dict(layout, sprites=layout["sprites"] * 2), self.root)
        sheet = textures.artwork_sheet([("sample", right)], 8, 1)
        self.assertEqual(set(sheet.crop((126, 5, 134, 13)).get_flattened_data()), {(18, 202, 42)})
        self.assertNotEqual(sheet.getpixel((6, 5)), (18, 202, 42))

    def test_asset_validation_and_export(self):
        source = self.root / "source"
        source.mkdir()
        runtime = self.root / "runtime"
        image = Image.new("RGBA", (16, 16), (20, 30, 40, 255))
        image.save(source / "tile.png")
        entry = dict(id="tile", source="source/tile.png", runtime="zed/textures/test/tile.png", size=[16, 16],
                     role="fixture", palette="fixture", origin="zto-original", jar=True, alpha="opaque")
        with self.assertRaisesRegex(ValueError, "Missing or stale"):
            textures.validate_assets([entry], self.root, runtime)
        textures.validate_assets([entry], self.root, runtime, export=True)
        destination = runtime / entry["runtime"]
        self.assertEqual(textures.validate_assets([entry], self.root, runtime), 1)
        saved = destination.read_bytes()
        textures.validate_assets([entry], self.root, runtime, export=True)
        self.assertEqual(destination.read_bytes(), saved)
        with self.assertRaisesRegex(ValueError, "invalid|Invalid"):
            textures.validate_assets([dict(entry, runtime="gui/tile.png")], self.root, runtime, export=True)
        with self.assertRaisesRegex(ValueError, "Unsafe"):
            textures.validate_assets([dict(entry, runtime="zed/textures/../../tile.png")],
                                     self.root, runtime, export=True)
        with self.assertRaisesRegex(ValueError, "duplicate"):
            textures.validate_assets([entry, entry], self.root, runtime)
        with self.assertRaisesRegex(ValueError, "Wrong PNG"):
            textures.validate_assets([dict(entry, size=[32, 32])], self.root, runtime)
        image.putpixel((0, 0), (5, 6, 7, 128))
        image.save(source / "tile.png")
        with self.assertRaisesRegex(ValueError, "Unexpected alpha"):
            textures.validate_assets([entry], self.root, runtime, export=True)
        self.assertEqual(destination.read_bytes(), saved)

    def test_reference_integrity_and_resource_filter(self):
        with self.assertRaisesRegex(ValueError, "Checksum mismatch"):
            textures.verified(b"source", "deadbeef", sha1, "fixture")
        path = self.root / "source.zip"
        with ZipFile(path, "w") as archive:
            archive.writestr("gui/items.png", b"png fixture")
            archive.writestr("gui/Test.java", b"source fixture")
            archive.writestr("unrelated.png", b"other fixture")
        output = self.root / "refs"
        textures.import_images(path, output, ["gui/"], {"archive": path.name})
        index = textures.read_json(output / "index.json")
        self.assertEqual(list(index["resources"]), ["gui/items.png"])
        self.assertEqual(index["archiveSha256"], textures.sha256(path.read_bytes()).hexdigest())
        self.assertFalse((output / "gui/Test.java").exists())


if __name__ == "__main__":
    unittest.main()
