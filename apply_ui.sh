#!/data/data/com.termux/files/usr/bin/bash
set -e
cd ~/duckcraft-launcher3

BASE="app_pojavlauncher/src/main"

# Thêm string mới (chỉ bản mặc định, tiếng Anh)
sed -i '/<\/resources>/i\    <string name="browse_content_button">Browse Mods \&amp; Resource Packs</string>\n    <string name="browse_content_title">What do you want to install?</string>\n    <string name="browse_content_mod">Mod</string>\n    <string name="browse_content_resourcepack">Resource Pack</string>' "$BASE/res/values/strings.xml"

echo "=== Kiểm tra kết quả ==="
grep -c "browse_content_button" "$BASE/res/values/strings.xml"
grep -c "ARG_CONTENT_TYPE" "$BASE/java/net/kdt/pojavlaunch/fragments/SearchModFragment.java"
grep -c "browse_content_button" "$BASE/res/layout/fragment_launcher.xml"
grep -c "openContentBrowser" "$BASE/java/net/kdt/pojavlaunch/fragments/MainMenuFragment.java"
echo "(Kỳ vọng: 1, 1(hoặc 2), 1, 2)"
