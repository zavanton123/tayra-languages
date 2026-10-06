#!/usr/bin/env python3
"""
Bundles the frequency-list courses of a language (tools/courses/<code>/*.json, written as
tools/courses/<code>/BRIEF.md describes) into one course pack, a SQLite file the app downloads
from Settings > Courses: course-packs/courses-<code>.sqlite, and the same gzip-compressed as
course-packs/courses-<code>.sqlite.gzip, which is the file to upload to the GitHub release the
app's CoursePacks catalog points at. Each course is checked with tools/check_course.py first.

Usage: python3 tools/build_courses.py --language pt [--only id,id...] [--force]
--only bundles just those courses (ones still being written stay out); --force bundles courses
that fail the check (their reports are still printed).

The pack's layout must match core/data/src/commonMain/sqldelight-coursepack (CoursePack.sq) and
FORMAT below must match CoursePack.FORMAT in the app. A course reaches a reader's database once:
change a course after it has shipped and readers who have it keep the old one, so give a
reworked course a new id.
"""
import argparse
import glob
import gzip
import io
import json
import os
import shutil
import sqlite3
import sys
from contextlib import redirect_stdout

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from check_course import check  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "course-packs")
FORMAT = 1

SCHEMA = """
CREATE TABLE meta (key TEXT NOT NULL PRIMARY KEY, value TEXT NOT NULL);
CREATE TABLE courses (
    id TEXT NOT NULL PRIMARY KEY,
    position INTEGER NOT NULL,
    rank_up_to INTEGER,
    level TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    topic TEXT NOT NULL
);
CREATE TABLE lessons (
    id TEXT NOT NULL PRIMARY KEY,
    course_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    title TEXT NOT NULL,
    summary TEXT NOT NULL,
    new_words TEXT NOT NULL,
    text TEXT NOT NULL
);
CREATE INDEX lessons_course ON lessons(course_id, position);
"""


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--language", required=True)
    parser.add_argument("--only")
    parser.add_argument("--force", action="store_true")
    args = parser.parse_args()
    paths = sorted(glob.glob(os.path.join(ROOT, "tools", "courses", args.language, f"{args.language}-*.json")))
    if args.only:
        wanted = set(args.only.split(","))
        paths = [p for p in paths if os.path.basename(p)[:-5] in wanted]
    courses = []
    failed = []
    for path in paths:
        buffer = io.StringIO()
        with redirect_stdout(buffer):
            ok = check(path, brief=True)
        if not ok:
            failed.append(os.path.basename(path))
            print(f"== {os.path.basename(path)} fails the check:\n{buffer.getvalue()}", file=sys.stderr)
            if not args.force:
                continue
        source = json.load(open(path, encoding="utf-8"))
        courses.append({
            "id": source["id"],
            "rankUpTo": source["rankUpTo"],
            "level": source["level"],
            "title": source["title"],
            "description": source["description"],
            "topic": source.get("topic", ""),
            "lessons": [
                {
                    "id": f"{source['id']}-{i:02}",
                    "title": lesson["title"],
                    "summary": lesson.get("summary", ""),
                    "newWords": lesson.get("newWords", []),
                    "text": lesson["text"].strip(),
                }
                for i, lesson in enumerate(source["lessons"], start=1)
            ],
        })
    courses.sort(key=lambda c: c["rankUpTo"])
    os.makedirs(OUT, exist_ok=True)
    out = os.path.join(OUT, f"courses-{args.language}.sqlite")
    write_pack(out, args.language, courses)
    with open(out, "rb") as source, gzip.open(out + ".gzip", "wb", compresslevel=9) as target:
        shutil.copyfileobj(source, target)
    print(f"{out}.gzip: {len(courses)} courses, {sum(len(c['lessons']) for c in courses)} lessons, "
          f"{os.path.getsize(out + '.gzip') / 1e6:.1f} MB compressed, {os.path.getsize(out) / 1e6:.1f} MB unpacked"
          + (f"; failing: {', '.join(failed)}" if failed else ""))
    return 1 if failed and not args.force else 0


def write_pack(path, language, courses):
    """Writes the courses into a new SQLite file whose user_version is the format, so the app's
    drivers neither create nor migrate it."""
    if os.path.exists(path):
        os.remove(path)
    db = sqlite3.connect(path)
    db.executescript(SCHEMA)
    db.executemany("INSERT INTO meta VALUES (?, ?)", [
        ("format", str(FORMAT)),
        ("language", language),
        ("courses", str(len(courses))),
        ("lessons", str(sum(len(c["lessons"]) for c in courses))),
    ])
    for position, course in enumerate(courses):
        db.execute(
            "INSERT INTO courses VALUES (?, ?, ?, ?, ?, ?, ?)",
            (course["id"], position, course["rankUpTo"], course["level"], course["title"], course["description"], course["topic"]),
        )
        db.executemany(
            "INSERT INTO lessons VALUES (?, ?, ?, ?, ?, ?, ?)",
            [(lesson["id"], course["id"], i, lesson["title"], lesson["summary"], " ".join(lesson["newWords"]), lesson["text"])
             for i, lesson in enumerate(course["lessons"])],
        )
    db.execute(f"PRAGMA user_version = {FORMAT}")
    db.commit()
    db.execute("VACUUM")
    db.close()


if __name__ == "__main__":
    sys.exit(main())
