#!/usr/bin/env python3
"""
Bundles the frequency-list courses of a language (tools/courses/<code>/*.json, written as
tools/courses/<code>/BRIEF.md describes) into the app, as
feature/courses/src/commonMain/composeResources/files/courses/<code>.json, after checking each
with tools/check_course.py. The app writes them into its database as sample courses.

Usage: python3 tools/build_courses.py --language pt [--only id,id...] [--force]
--only bundles just those courses (ones still being written stay out); --force bundles courses
that fail the check (their reports are still printed).

A course reaches a reader's database once: change a course after it has shipped and readers who
have it keep the old one, so give a reworked course a new id.
"""
import argparse
import glob
import io
import json
import os
import sys
from contextlib import redirect_stdout

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from check_course import check  # noqa: E402

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "feature", "courses", "src", "commonMain", "composeResources", "files", "courses")


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
    out = os.path.join(OUT, f"{args.language}.json")
    with open(out, "w", encoding="utf-8") as f:
        json.dump({"courses": courses}, f, ensure_ascii=False, indent=1)
    print(f"{out}: {len(courses)} courses, {sum(len(c['lessons']) for c in courses)} lessons" + (f"; failing: {', '.join(failed)}" if failed else ""))
    return 1 if failed and not args.force else 0


if __name__ == "__main__":
    sys.exit(main())
