#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
BASE="app_pojavlauncher/src/main"
FILE="$BASE/java/net/kdt/pojavlaunch/fragments/SearchModFragment.java"

sed -i -z 's|mImportLauncher\.launch("\*/\*");\n        });|mImportLauncher.launch("*/*");\n        });\n\n        if (mSearchFilters.isModpack) {\n            mSearchEditText.setHint(R.string.hint_search_modpack);\n            mImportButton.setVisibility(View.VISIBLE);\n        } else {\n            mImportButton.setVisibility(View.GONE);\n            mSearchEditText.setHint(mSearchFilters.isResourcePack ? R.string.hint_search_resourcepack : R.string.hint_search_mod);\n        }|' "$FILE"

sed -i '/<\/resources>/i\    <string name="hint_search_mod">Search mods...</string>\n    <string name="hint_search_resourcepack">Search resource packs...</string>' "$BASE/res/values/strings.xml"

echo "=== Kiểm tra ==="
grep -c "hint_search_mod\b" "$FILE"
grep -c "hint_search_resourcepack" "$BASE/res/values/strings.xml"
