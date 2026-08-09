#!/data/data/com.termux/files/usr/bin/bash
cd ~/duckcraft-launcher3

LAYOUT="app_pojavlauncher/src/main/res/layout/fragment_mod_search.xml"
JAVAFILE="app_pojavlauncher/src/main/java/net/kdt/pojavlaunch/fragments/SearchModFragment.java"
STRINGS="app_pojavlauncher/src/main/res/values/strings.xml"

# 1. Thêm TextView cảnh báo vào layout, dưới progress bar
sed -i 's|            tools:layout_editor_absoluteX="13dp" />|            tools:layout_editor_absoluteX="13dp" />\n\n        <TextView\n            android:id="@+id/dependency_warning_text"\n            android:layout_width="match_parent"\n            android:layout_height="wrap_content"\n            android:layout_marginTop="@dimen/padding_moderate"\n            android:text="@string/mod_dependency_warning"\n            android:textColor="@color/accent_green"\n            android:textSize="@dimen/_11ssp"\n            android:visibility="gone"\n            app:layout_constraintTop_toBottomOf="@id/search_mod_progressbar" />|' "$LAYOUT"

# 2. Thêm string cảnh báo
sed -i '/<\/resources>/i\    <string name="mod_dependency_warning">⚠️ Some mods require other mods to work. This does not download dependencies automatically — check the mod page if unsure.</string>' "$STRINGS"

# 3. Thêm logic ẩn/hiện trong SearchModFragment.java
sed -i -z 's|mImportButton.setVisibility(View.GONE);\n            mSearchEditText.setHint(mSearchFilters.isResourcePack ? R.string.hint_search_resourcepack : R.string.hint_search_mod);\n        }|mImportButton.setVisibility(View.GONE);\n            mSearchEditText.setHint(mSearchFilters.isResourcePack ? R.string.hint_search_resourcepack : R.string.hint_search_mod);\n        }\n\n        View dependencyWarning = view.findViewById(R.id.dependency_warning_text);\n        dependencyWarning.setVisibility(!mSearchFilters.isModpack \&\& !mSearchFilters.isResourcePack ? View.VISIBLE : View.GONE);|' "$JAVAFILE"

echo "=== Kiểm tra ==="
grep -c "dependency_warning_text" "$LAYOUT"
grep -c "mod_dependency_warning" "$STRINGS"
grep -c "dependencyWarning" "$JAVAFILE"
echo "(Kỳ vọng: 1, 1, 2)"
