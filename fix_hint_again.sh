#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3
FILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/fragments/SearchModFragment.java"

sed -i -z 's|mImportLauncher\.launch("\*/\*");\n        });|mImportLauncher.launch("*/*");\n        });\n\n        if (mSearchFilters.isModpack) {\n            mSearchEditText.setHint(R.string.hint_search_modpack);\n            mImportButton.setVisibility(View.VISIBLE);\n        } else {\n            mImportButton.setVisibility(View.GONE);\n            mSearchEditText.setHint(mSearchFilters.isResourcePack ? R.string.hint_search_resourcepack : R.string.hint_search_mod);\n        }|' "$FILE"

echo "=== Kiểm tra ==="
grep -c "hint_search_mod\b" "$FILE"
grep -c "mImportButton.setVisibility" "$FILE"
