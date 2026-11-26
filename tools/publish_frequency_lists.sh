#!/usr/bin/env bash
# Uploads the frequency lists to the GitHub release the Android app downloads them from
# (FrequencyListFiles.URL). The Android app does not bundle them, to keep its download small;
# the other platforms still read them from the app's resources.
#
# Run it after changing any list. A list that changed for good, rather than one fixed in place,
# goes to a new release: raise FrequencyListFiles.VERSION and pass the new number here.
#
#   tools/publish_frequency_lists.sh 1
set -euo pipefail
version="${1:?usage: $0 <version>}"
tag="frequency-v$version"
root="$(cd "$(dirname "$0")/.." && pwd)"
lists="$root/feature/frequency/src/commonMain/composeResources/files/frequency"
out="$(mktemp -d)"
trap 'rm -rf "$out"' EXIT
for list in "$lists"/*.tsv; do
    gzip -9 -n -c "$list" > "$out/$(basename "$list").gz"
done
if ! gh release view "$tag" >/dev/null 2>&1; then
    gh release create "$tag" --title "Frequency lists v$version" --latest=false \
        --notes "The word frequency lists (the 10,000 most common words of each language, as tab-separated text), downloaded by the Android app when a language needs one. Uploaded with tools/publish_frequency_lists.sh."
fi
gh release upload "$tag" "$out"/*.tsv.gz --clobber
echo "Uploaded $(ls "$out" | wc -l | tr -d ' ') lists to $tag"
