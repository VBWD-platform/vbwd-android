#!/usr/bin/env bash
#
# Regenerate docs/html/ from docs/developer/md/ (the markdown is the source of
# truth). Renders each page with a shared sidebar nav, rewrites cross-links
# .md -> .html, and copies the screenshots.
#
# Usage:  docs/build-html.sh        (requires: pandoc, perl)
set -euo pipefail

DOCS="$(cd "$(dirname "$0")" && pwd)"
MD="$DOCS/developer/md"
OUT="$DOCS/html"

command -v pandoc >/dev/null || { echo "pandoc is required (brew install pandoc)"; exit 1; }

# Ordered pages: "<basename>:<nav label>". Add a page here + a matching
# <basename>.md and it appears in every sidebar.
PAGES=(
    "index:Overview"
    "getting-started:Getting started"
    "architecture:Architecture"
    "plugin-contract:Plugin contract"
    "writing-a-plugin:Writing a plugin"
    "consuming-the-sdk:Consuming the SDK"
    "host-app:Host app"
    "testing:Testing &amp; the gate"
    "meinchat-reference:MeinChat reference"
    "artifacts:Artifacts"
)

mkdir -p "$OUT/screenshots"
cp -f "$MD/screenshots/"*.png "$OUT/screenshots/" 2>/dev/null || true

# --- build the shared sidebar nav ------------------------------------------
nav=""
for entry in "${PAGES[@]}"; do
    base="${entry%%:*}"; label="${entry#*:}"
    nav+="  <a href=\"$base.html\">$label</a>"$'\n'
done

# --- assemble the pandoc template (head + nav + tail) ----------------------
tmpl="$(mktemp)"
cat > "$tmpl" <<'HEAD'
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>$title$ · vbwd-android SDK</title>
<style>
  :root{--bg:#0f1320;--panel:#161c2d;--ink:#e7ebf3;--mut:#9aa6bf;--acc:#3b82f6;--code:#0b0f1a;--line:#26304a}
  *{box-sizing:border-box}
  body{margin:0;font:16px/1.65 -apple-system,BlinkMacSystemFont,"Segoe UI",Roboto,Helvetica,Arial,sans-serif;color:var(--ink);background:var(--bg)}
  .sidebar{position:fixed;top:0;left:0;width:250px;height:100vh;overflow:auto;background:var(--panel);border-right:1px solid var(--line);padding:22px 16px}
  .sidebar .brand{font-weight:700;font-size:15px;letter-spacing:.02em;color:#fff;margin:0 8px 16px;padding-bottom:14px;border-bottom:1px solid var(--line)}
  .sidebar a{display:block;color:var(--mut);text-decoration:none;padding:7px 10px;border-radius:8px;font-size:14.5px}
  .sidebar a:hover{background:#1d2741;color:var(--ink)}
  main{margin-left:250px;max-width:860px;padding:42px 48px 80px}
  h1,h2,h3{color:#fff;line-height:1.25;font-weight:680}
  h1{font-size:30px;margin:.2em 0 .6em;border-bottom:1px solid var(--line);padding-bottom:.3em}
  h2{font-size:22px;margin:1.8em 0 .6em} h3{font-size:17px;margin:1.4em 0 .4em}
  a{color:var(--acc)} a:hover{text-decoration:underline}
  code{background:var(--code);border:1px solid var(--line);border-radius:5px;padding:.1em .4em;font:13.5px/1.5 "SF Mono",ui-monospace,Menlo,Consolas,monospace}
  pre{background:var(--code);border:1px solid var(--line);border-radius:10px;padding:16px 18px;overflow:auto}
  pre code{background:none;border:0;padding:0}
  table{border-collapse:collapse;width:100%;margin:1em 0;font-size:14.5px}
  th,td{border:1px solid var(--line);padding:8px 12px;text-align:left;vertical-align:top}
  th{background:#1d2741} tr:nth-child(even) td{background:#131a2a}
  blockquote{border-left:3px solid var(--acc);margin:1em 0;padding:.4em 1em;background:#131a2a;border-radius:0 8px 8px 0;color:var(--mut)}
  main img{max-width:270px;border:1px solid var(--line);border-radius:14px;box-shadow:0 8px 30px rgba(0,0,0,.4);margin:10px 0}
  hr{border:0;border-top:1px solid var(--line);margin:2em 0}
</style>
</head>
<body>
<nav class="sidebar">
  <div class="brand">vbwd-android SDK</div>
HEAD
printf '%s' "$nav" >> "$tmpl"
cat >> "$tmpl" <<'TAIL'
</nav>
<main>
$body$
</main>
</body>
</html>
TAIL

# --- render each page ------------------------------------------------------
count=0
for entry in "${PAGES[@]}"; do
    base="${entry%%:*}"
    src="$MD/$base.md"
    [ -f "$src" ] || { echo "WARN: missing $src — skipping"; continue; }
    pandoc "$src" --from gfm --to html5 --standalone --template "$tmpl" -o "$OUT/$base.html"
    # cross-links: foo.md(#frag) -> foo.html(#frag)
    perl -i -pe 's{href="([^"]+)\.md(#[^"]*)?"}{"href=\"$1.html".($2//"")."\""}ge' "$OUT/$base.html"
    count=$((count + 1))
done

rm -f "$tmpl"
echo "✅ rendered $count page(s) → $OUT (open $OUT/index.html)"
