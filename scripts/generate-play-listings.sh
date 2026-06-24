#!/usr/bin/env bash
# Generate the Gradle Play Publisher listing tree (app/src/main/play) from the
# project's canonical metadata under metadata/play. Android-style locale folders
# are mapped to the BCP-47 codes Google Play expects.
#
# Source of truth: metadata/play/**  ->  generated: app/src/main/play/**  (gitignored)
set -euo pipefail
cd "$(dirname "$0")/.."

SRC=metadata/play
DEST=app/src/main/play
TITLE='SMS Backup+'
FEATURE="$SRC/assets/feature_graphic.png"
ICON="$SRC/assets/sms_512x512.png"

# Android res qualifier -> Play BCP-47 locale
declare -A LOCALE=(
  [da]=da-DK [de]=de-DE [en]=en-US [fr]=fr-FR [gl]=gl-ES [ko]=ko-KR
  [nl]=nl-NL [pl]=pl-PL [tr]=tr-TR [zh-rCN]=zh-CN [zh-rTW]=zh-TW
)

rm -rf "$DEST/listings"
for lang in "${!LOCALE[@]}"; do
  loc="${LOCALE[$lang]}"
  txt="$SRC/text/$lang"
  [ -d "$txt" ] || { echo "skip $lang (no text)"; continue; }

  base="$DEST/listings/$loc"
  mkdir -p "$base/graphics/phone-screenshots" "$base/graphics/feature-graphic" "$base/graphics/icon"

  printf '%s' "$TITLE" > "$base/title.txt"
  cp "$txt/short_description.txt" "$base/short-description.txt"
  cp "$txt/full_description.txt"  "$base/full-description.txt"

  # localized screenshots (numbered files keep their order)
  if compgen -G "$SRC/screenshots/$lang/*.png" > /dev/null; then
    cp "$SRC/screenshots/$lang"/*.png "$base/graphics/phone-screenshots/"
  fi

  # shared assets (one file each, as GPP requires)
  cp "$FEATURE" "$base/graphics/feature-graphic/feature.png"
  cp "$ICON"    "$base/graphics/icon/icon.png"

  echo "generated $loc"
done
echo "Done -> $DEST/listings"
