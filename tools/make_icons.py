#!/usr/bin/env python3
"""Generates every platform icon from the monkey logo.

Usage: python3 tools/make_icons.py "<path to logo png>"
The logo png is tools/logo/idiomiko-monkey.svg rendered on a transparent background, 2048 px or so
(a browser can render it). Needs Pillow and, on macOS, iconutil for the icns.
"""
import pathlib
import subprocess
import sys
import tempfile

from PIL import Image, ImageDraw

CREAM = (250, 246, 243)
ROOT = pathlib.Path(__file__).resolve().parent.parent


def load_logo(path):
    logo = Image.open(path).convert("RGBA")
    return logo.crop(logo.getchannel("A").getbbox())


def place(canvas, logo, box, center, scale):
    """Draws the logo with its longest side at scale*box, centred at center."""
    target = int(box * scale)
    ratio = target / max(logo.size)
    art = logo.resize((max(1, int(logo.width * ratio)), max(1, int(logo.height * ratio))), Image.LANCZOS)
    canvas.alpha_composite(art, (int(center[0] - art.width / 2), int(center[1] - art.height / 2)))


def square(logo, size, scale=0.9):
    im = Image.new("RGBA", (size, size), CREAM + (255,))
    place(im, logo, size, (size / 2, size / 2), scale)
    return im


def rounded(logo, size, scale=0.84, radius_ratio=0.22, inset_ratio=0.1):
    """macOS style: a rounded plate with transparent margins."""
    im = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    inset = int(size * inset_ratio)
    box = size - 2 * inset
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle((inset, inset, inset + box, inset + box), radius=int(box * radius_ratio), fill=255)
    im.paste(Image.new("RGBA", (size, size), CREAM + (255,)), (0, 0), mask)
    place(im, logo, box, (size / 2, size / 2), scale)
    return im


def save(im, rel):
    path = ROOT / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    im.save(path)
    print(rel)


def main(logo):
    logo = load_logo(logo)

    # Android: adaptive foreground per density (art within the 66dp safe zone) plus legacy icons.
    for name, mult in {"mdpi": 1, "hdpi": 1.5, "xhdpi": 2, "xxhdpi": 3, "xxxhdpi": 4}.items():
        fg = int(108 * mult)
        layer = Image.new("RGBA", (fg, fg), (0, 0, 0, 0))
        place(layer, logo, fg, (fg / 2, fg / 2), 0.6)
        save(layer, f"androidApp/src/main/res/mipmap-{name}/ic_launcher_foreground.png")
        legacy = int(48 * mult)
        save(square(logo, legacy), f"androidApp/src/main/res/mipmap-{name}/ic_launcher.png")
        mask = Image.new("L", (legacy, legacy), 0)
        ImageDraw.Draw(mask).ellipse((0, 0, legacy - 1, legacy - 1), fill=255)
        disc = Image.new("RGBA", (legacy, legacy), (0, 0, 0, 0))
        disc.paste(square(logo, legacy, 0.78), (0, 0), mask)
        save(disc, f"androidApp/src/main/res/mipmap-{name}/ic_launcher_round.png")

    save(square(logo, 1024).convert("RGB"), "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/AppIcon.png")

    with tempfile.TemporaryDirectory() as tmp:
        iconset = pathlib.Path(tmp) / "Idiomiko.iconset"
        iconset.mkdir()
        for base in (16, 32, 128, 256, 512):
            rounded(logo, base).save(iconset / f"icon_{base}x{base}.png")
            rounded(logo, base * 2).save(iconset / f"icon_{base}x{base}@2x.png")
        subprocess.run(["iconutil", "-c", "icns", str(iconset), "-o", str(ROOT / "desktopApp/icons/Idiomiko.icns")], check=True)
        print("desktopApp/icons/Idiomiko.icns")
    square(logo, 256).save(ROOT / "desktopApp/icons/Idiomiko.ico", sizes=[(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    print("desktopApp/icons/Idiomiko.ico")
    save(square(logo, 512), "desktopApp/icons/Idiomiko.png")
    save(square(logo, 256), "desktopApp/src/main/resources/window-icon.png")
    save(rounded(logo, 1024), "desktopApp/src/main/resources/dock-icon.png")

    save(square(logo, 64), "webApp/src/wasmJsMain/resources/favicon.png")
    save(square(logo, 180).convert("RGB"), "webApp/src/wasmJsMain/resources/apple-touch-icon.png")
    save(square(logo, 512), "webApp/src/wasmJsMain/resources/icon-512.png")

    for size in (16, 32, 48, 96, 128):
        save(rounded(logo, size, inset_ratio=0.04), f"browser-extension/public/icon/{size}.png")


if __name__ == "__main__":
    main(sys.argv[1])
