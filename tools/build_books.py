#!/usr/bin/env python3
"""
Bundles the books of a language (tools/books/<code>/books.json with the texts fetched by
tools/fetch_books.py into tools/books/<code>/text/) into one book pack, a SQLite file the app
downloads from Settings > Books: book-packs/books-<code>.sqlite, and the same gzip-compressed as
book-packs/books-<code>.sqlite.gzip, which is the file to upload to the GitHub release the app's
BookPacks catalog points at.

Usage: python3 tools/build_books.py --language pt

The pack's layout must match core/data/src/commonMain/sqldelight-bookpack (BookPack.sq) and
FORMAT below must match BookPack.FORMAT in the app. A book reaches a reader's library once, by
its id: change a book after it has shipped and readers who have it keep the old one, so give a
reworked book a new id.
"""
import argparse
import gzip
import json
import os
import re
import shutil
import sqlite3

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "book-packs")
FORMAT = 1

SCHEMA = """
CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE books (
    id TEXT NOT NULL PRIMARY KEY,
    position INTEGER NOT NULL,
    level TEXT NOT NULL,
    title TEXT NOT NULL,
    author TEXT NOT NULL,
    summary TEXT NOT NULL,
    source_url TEXT NOT NULL,
    license TEXT NOT NULL,
    tags TEXT NOT NULL,
    text TEXT NOT NULL
);
"""


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--language", required=True)
    args = parser.parse_args()
    folder = os.path.join(ROOT, "tools", "books", args.language)
    manifest = json.load(open(os.path.join(folder, "books.json"), encoding="utf-8"))
    books = []
    for book in manifest["books"]:
        path = os.path.join(folder, "text", f"{book['id']}.txt")
        if not os.path.exists(path):
            raise SystemExit(f"{book['id']}: no text at {os.path.relpath(path, ROOT)}; run tools/fetch_books.py first")
        text = open(path, encoding="utf-8").read().strip()
        words = len(re.findall(r"\w+", text))
        books.append((book, text, words))
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, f"books-{args.language}.sqlite")
    write_pack(out, args.language, books)
    with open(out, "rb") as source, gzip.open(out + ".gzip", "wb", compresslevel=9) as target:
        shutil.copyfileobj(source, target)
    total = sum(words for _, _, words in books)
    print(f"{out}.gzip: {len(books)} books, {total:,} words, {os.path.getsize(out + '.gzip') / 1e6:.1f} MB compressed, {os.path.getsize(out) / 1e6:.1f} MB unpacked")
    for book, _, words in books:
        print(f"  {book['level']}  {book['title']:<45} {words:>8,} words")
    print(f"downloadSize for BookPacks.kt: {os.path.getsize(out + '.gzip')}")


def write_pack(path, language, books):
    if os.path.exists(path):
        os.remove(path)
    db = sqlite3.connect(path)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO meta VALUES (?, ?)", [("format", str(FORMAT)), ("language", language), ("books", str(len(books)))])
    for position, (book, text, _) in enumerate(books):
        db.execute(
            "INSERT INTO books VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
            (book["id"], position, book["level"], book["title"], book["author"], book["summary"], book["url"], book["license"], " ".join(book.get("tags", [])), text),
        )
    db.execute(f"PRAGMA user_version = {FORMAT}")
    db.commit()
    db.execute("VACUUM")
    db.close()


if __name__ == "__main__":
    main()
