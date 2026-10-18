#!/usr/bin/env python3
"""
Writes a releases list in the shape of GitHub's API (`releases`, in the current folder) that offers
the installers given as one release, for trying the desktop app's updates without publishing
one; .github/workflows/update-check.yml serves it with `python3 -m http.server`. A data release
comes first, as on GitHub, to show it is passed over.

Usage: python3 tools/make_update_feed.py <version> <base URL> <installer>...
"""
import hashlib
import json
import os
import sys


def main():
    version, base, files = sys.argv[1], sys.argv[2].rstrip("/"), sys.argv[3:]
    assets = []
    for path in files:
        with open(path, "rb") as f:
            digest = hashlib.sha256(f.read()).hexdigest()
        name = os.path.basename(path)
        assets.append({"name": name, "size": os.path.getsize(path), "digest": f"sha256:{digest}", "browser_download_url": f"{base}/{name}"})
    releases = [
        {"tag_name": "books-v1", "draft": False, "prerelease": False, "html_url": "", "body": "", "assets": []},
        {"tag_name": f"v{version}", "draft": False, "prerelease": False, "html_url": "", "body": "Update check", "assets": assets},
    ]
    with open("releases", "w") as f:
        json.dump(releases, f, indent=1)


if __name__ == "__main__":
    main()
