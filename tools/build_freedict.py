#!/usr/bin/env python3
"""
Builds offline dictionary packs for the app from FreeDict's dictionaries (https://freedict.org),
the free bilingual dictionaries KOReader, GoldenDict and dictd users install.

FreeDict lists every dictionary and its releases in freedict-database.json; each is published as
StarDict files in a .tar.xz, which this script downloads (checking its SHA-512), unpacks and
turns into a pack with `tayra dictionaries convert`, the app's own StarDict import, so a pack
reads exactly as the same dictionary imported in the app would. The pack's meta then names
FreeDict, the dictionary's edition and its licence, as found in its .ifo description; a
dictionary whose licence is not recognised as a free one is left out and reported.

A FreeDict pack is called "<source>-<target>-freedict" by the app's language codes (FreeDict
names them by ISO 639-3), so it sits beside the Wiktionary pack of the same pair rather than in
its place; pairs with a language the app does not know are left out, and so are dictionaries with
fewer than --min-entries headwords.

Downloads are cached in dictionaries/freedict/, packs written to dictionaries/, and built packs
listed in dictionaries/freedict/built.json; --write-catalog writes that list into the app's catalog
(DictionaryPackSizes.kt) as the packs of the freedict-v1 release, where the files are published.

The conversion runs the CLI from cli/build/install (./gradlew :cli:installDist) on a data folder
of its own, so the library is never touched.

Usage: python3 tools/build_freedict.py [--only deu-eng,fra-eng] [--min-entries 1000]
       python3 tools/build_freedict.py --list
       python3 tools/build_freedict.py --write-catalog
"""
import argparse
import gzip
import hashlib
import json
import os
import re
import shutil
import sqlite3
import subprocess
import sys
import tarfile
import tempfile

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "dictionaries")
CACHE = os.path.join(OUT, "freedict")
BUILT = os.path.join(CACHE, "built.json")
CATALOG = os.path.join(ROOT, "core/domain/src/commonMain/kotlin/com/tayra/languages/core/domain/dictionary/DictionaryPackSizes.kt")
DATABASE_URL = "https://freedict.org/freedict-database.json"
RELEASE = "freedict-v1"
CLI = os.path.join(ROOT, "cli/build/install/tayra/bin/tayra")
USER_AGENT = "TayraLanguages/1.0 (https://github.com/zavanton123/tayra-languages)"

# FreeDict's ISO 639-3 codes, by the app's codes for them; the others are languages the app does not know.
CODES = {
    "afr": "af", "ara": "ar", "bre": "br", "bul": "bg", "cat": "ca", "ces": "cs", "cym": "cy", "dan": "da",
    "deu": "de", "ell": "el", "eng": "en", "epo": "eo", "fin": "fi", "fra": "fr", "gla": "gd", "gle": "ga",
    "hin": "hi", "hrv": "hr", "hun": "hu", "ind": "id", "isl": "is", "ita": "it", "jpn": "ja", "lat": "la",
    "lit": "lt", "mkd": "mk", "nld": "nl", "nob": "no", "nor": "no", "pol": "pl", "por": "pt", "rus": "ru",
    "san": "sa", "slk": "sk", "slv": "sl", "spa": "es", "srp": "sr", "swe": "sv", "swh": "sw", "tur": "tr",
    "zho": "zh",
}

# Licences by the links and words their descriptions use; the first that matches names it.
LICENSES = [
    (r"gnu\.org/licenses/(?:old-licenses/)?agpl-3", "AGPL-3.0"),
    (r"gnu\.org/licenses/(?:old-licenses/)?gpl-3|GNU General Public License,? (?:ver(?:sion|\.)? )?3", "GPL-3.0"),
    (r"gnu\.org/licenses/(?:old-licenses/)?gpl-2|GNU General Public License,? (?:ver(?:sion|\.)? )?2", "GPL-2.0+"),
    (r"gnu\.org/licenses/(?:old-licenses/)?lgpl", "LGPL"),
    (r"gnu\.org/licenses/(?:old-licenses/)?fdl|GNU Free Documentation License", "GFDL"),
    (r"creativecommons\.org/licenses/by-sa/([\d.]+)|Creative Commons Attribution[- ]Share[- ]?Alike(?: ([\d.]+))?", "CC BY-SA"),
    (r"creativecommons\.org/licenses/by/([\d.]+)|Creative Commons Attribution(?: ([\d.]+))?", "CC BY"),
    (r"creativecommons\.org/publicdomain/zero|CC0", "CC0"),
    (r"GNU General Public License|\bGPL\b", "GPL"),
    (r"\bpublic domain\b", "public domain"),
    (r"\bBSD\b", "BSD"),
    (r"\bMIT\b", "MIT"),
]


def fetch(url, path):
    if not os.path.exists(path):
        # curl uses the system's certificates, which a python.org Python on macOS does not.
        subprocess.run(["curl", "-fsSL", "-A", USER_AGENT, "-o", path + ".part", url], check=True)
        os.replace(path + ".part", path)
    return path


def database():
    return [d for d in json.load(open(fetch(DATABASE_URL, os.path.join(CACHE, "freedict-database.json")))) if "name" in d]


def stardict_release(dictionary):
    return next((r for r in dictionary["releases"] if r["platform"] == "stardict"), None)


def pair(dictionary):
    """The app's codes for the dictionary's languages, or None when the app does not know one of them."""
    source, target = dictionary["name"].split("-")
    if source not in CODES or target not in CODES or CODES[source] == CODES[target]:
        return None
    return CODES[source], CODES[target]


def license_of(description):
    for pattern, name in LICENSES:
        found = re.search(pattern, description, re.I)
        if found:
            version = next((g for g in found.groups() if g), None) if found.groups() else None
            return f"{name} {version}" if version else name
    return None


def unpack(archive, into):
    """The StarDict files of a FreeDict archive, by their plain names, written into [into]."""
    paths = []
    with tarfile.open(archive, "r:xz") as tar:
        for member in tar.getmembers():
            name = os.path.basename(member.name)
            if not member.isfile() or not re.search(r"\.(ifo|idx|idx\.gz|dict|dict\.dz|syn)$", name):
                continue
            path = os.path.join(into, name)
            with tar.extractfile(member) as source, open(path, "wb") as f:
                shutil.copyfileobj(source, f)
            paths.append(path)
    return paths


def ifo(paths):
    path = next(p for p in paths if p.endswith(".ifo"))
    lines = open(path, encoding="utf-8-sig").read().splitlines()[1:]
    return dict(line.split("=", 1) for line in lines if "=" in line)


def build(dictionary, min_entries):
    """Builds one dictionary's pack; returns (name, size), or None with the reason printed."""
    name = dictionary["name"]
    codes = pair(dictionary)
    release = stardict_release(dictionary)
    if codes is None or release is None:
        return None
    if int(dictionary.get("headwords") or 0) < min_entries:
        print(f"{name}: {dictionary.get('headwords')} headwords, fewer than {min_entries}; left out", file=sys.stderr)
        return None
    source, target = codes
    pack = f"{source}-{target}-freedict"
    archive = fetch(release["URL"], os.path.join(CACHE, os.path.basename(release["URL"])))
    digest = hashlib.sha512(open(archive, "rb").read()).hexdigest()
    if digest != release["checksum"]:
        os.remove(archive)
        sys.exit(f"{name}: the download's SHA-512 is not FreeDict's; removed it, run again")
    with tempfile.TemporaryDirectory() as work:
        files = os.path.join(work, "files")
        os.mkdir(files)
        paths = unpack(archive, files)
        header = ifo(paths)
        description = re.sub(r"<[^>]+>", " ", header.get("description", "").replace("&lt;", "<").replace("&gt;", ">"))
        licence = license_of(header.get("description", "")) or license_of(description)
        if licence is None:
            print(f"{name}: no free licence recognised in its description; left out", file=sys.stderr)
            return None
        sqlite_path = os.path.join(work, pack + ".sqlite")
        result = subprocess.run(
            [CLI, "--json", "--data-dir", os.path.join(work, "data"), "dictionaries", "convert", *paths,
             "--from", source, "--to", target, "--out", sqlite_path],
            capture_output=True, text=True,
        )
        if result.returncode != 0:
            print(f"{name}: not converted: {result.stderr.strip()}", file=sys.stderr)
            return None
        title = header.get("bookname", name).strip()
        db = sqlite3.connect(sqlite_path)
        sources = f"{title if 'FreeDict' in title else 'FreeDict ' + title}, edition {dictionary.get('edition', release['version'])} (https://freedict.org/, {licence})"
        db.execute("UPDATE meta SET value = ? WHERE key = 'sources'", (sources,))
        db.execute("INSERT OR REPLACE INTO meta(key, value) VALUES ('license', ?)", (licence,))
        entries = int(db.execute("SELECT value FROM meta WHERE key = 'entry_count'").fetchone()[0])
        db.commit()
        db.execute("VACUUM")
        db.close()
        if entries < min_entries:
            print(f"{name}: {entries} entries with glosses, fewer than {min_entries}; left out", file=sys.stderr)
            return None
        out_path = os.path.join(OUT, pack + ".sqlite.gzip")
        with open(sqlite_path, "rb") as plain, gzip.open(out_path, "wb", compresslevel=9) as packed:
            shutil.copyfileobj(plain, packed)
    size = os.path.getsize(out_path)
    print(f"{pack}: {entries} entries, {size:,} bytes, {licence} ({name} {dictionary.get('edition')})")
    return pack, size


def record(name, size):
    built = json.load(open(BUILT)) if os.path.exists(BUILT) else {}
    built[name] = size
    with open(BUILT, "w") as f:
        json.dump(dict(sorted(built.items())), f, indent=1)


def write_catalog():
    """Writes the packs built so far into the app's catalog as the freedict-v1 block, adding the block when it is not there."""
    built = json.load(open(BUILT)) if os.path.exists(BUILT) else {}
    text = open(CATALOG).read()
    items = [f'"{name}" to {size:_}L' for name, size in sorted(built.items())]
    lines = "\n".join("        " + ", ".join(items[i:i + 4]) + "," for i in range(0, len(items), 4))
    block = re.compile(r'(    "' + RELEASE + r'" to mapOf\(\n)(.*?)(\n    \),)', re.S)
    if block.search(text):
        text = block.sub(lambda m: m.group(1) + lines + m.group(3), text)
    else:
        text = re.sub(r"\n\)\n$", f'\n    "{RELEASE}" to mapOf(\n{lines}\n    ),\n)\n', text)
    with open(CATALOG, "w") as f:
        f.write(text)
    print(f"wrote {len(items)} packs into the {RELEASE} block", file=sys.stderr)


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--only", help="comma-separated FreeDict names to build, such as deu-eng")
    parser.add_argument("--min-entries", type=int, default=1000, help="leave out dictionaries with fewer entries")
    parser.add_argument("--list", action="store_true", help="print the dictionaries that would be built")
    parser.add_argument("--write-catalog", action="store_true", help="write the built packs into the app's catalog")
    args = parser.parse_args()
    os.makedirs(CACHE, exist_ok=True)
    if args.write_catalog:
        write_catalog()
        return
    wanted = set(args.only.split(",")) if args.only else None
    dictionaries = [d for d in database() if pair(d) and stardict_release(d) and (wanted is None or d["name"] in wanted)]
    # Two FreeDict dictionaries of one pair (nor-eng and nob-eng): the one with more headwords.
    best = {}
    for d in dictionaries:
        key = pair(d)
        if key not in best or int(d.get("headwords") or 0) > int(best[key].get("headwords") or 0):
            best[key] = d
    dictionaries = sorted(best.values(), key=lambda d: d["name"])
    if args.list:
        for d in dictionaries:
            release = stardict_release(d)
            print(f"{d['name']}\t{'-'.join(pair(d))}\t{d.get('headwords')}\t{release['size']}\t{d.get('status')}")
        print(f"{len(dictionaries)} dictionaries, {sum(int(stardict_release(d)['size']) for d in dictionaries):,} bytes to download", file=sys.stderr)
        return
    if not os.path.exists(CLI):
        sys.exit("No CLI at cli/build/install; run ./gradlew :cli:installDist first")
    for d in dictionaries:
        built = build(d, args.min_entries)
        if built:
            record(*built)


if __name__ == "__main__":
    main()
